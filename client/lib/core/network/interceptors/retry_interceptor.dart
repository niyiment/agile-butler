
import 'package:dio/dio.dart';
import 'package:logger/logger.dart';

class RetryInterceptor extends Interceptor {

  RetryInterceptor({
    required Dio dio,
    required Logger logger,
    this.maxRetries = 3,
    this.initialDelay = const Duration(seconds: 1),
    this.retryStatusCodes = const {500, 502, 503, 504},
  })  : _dio = dio,
        _logger = logger;
  final Dio _dio;
  final Logger _logger;
  final int maxRetries;
  final Duration initialDelay;
  final Set<int> retryStatusCodes;

  @override
  Future<void> onError(
      DioException err,
      ErrorInterceptorHandler handler,
      ) async {
    final requestOptions = err.requestOptions;

    if (_shouldSkipRetry(err, requestOptions)) {
      return handler.next(err);
    }

    final currentAttempt =
        (requestOptions.extra['retryCount'] as int?) ?? 0;

    if (currentAttempt >= maxRetries) {
      _logger.w(
        'Max retries ($maxRetries) reached for ${requestOptions.path}',
      );
      return handler.next(err);
    }

    // Exponential backoff: 1s, 2s, 4s
    final delay = initialDelay * (1 << currentAttempt);
    _logger.i(
      'Retrying request to ${requestOptions.path} '
          '(attempt ${currentAttempt + 1}/$maxRetries) after ${delay.inSeconds}s',
    );

    await Future.delayed(delay);

    try {
      final retryOptions = Options(
        method: requestOptions.method,
        headers: requestOptions.headers,
        contentType: requestOptions.contentType,
        extra: {
          ...requestOptions.extra,
          'retryCount': currentAttempt + 1,
        },
      );

      final response = await _dio.request(
        requestOptions.path,
        data: requestOptions.data,
        queryParameters: requestOptions.queryParameters,
        options: retryOptions,
      );

      handler.resolve(response);
    } on DioException catch (retryError) {
      handler.next(retryError);
    }
  }

  bool _shouldSkipRetry(DioException err, RequestOptions options) {
    if (options.extra['disableRetry'] == true) return true;

    if (err.type == DioExceptionType.cancel) return true;

    if (err.response != null &&
        !retryStatusCodes.contains(err.response!.statusCode)) {
      return true;
    }

    final method = options.method.toUpperCase();
    if (method == 'POST' && options.extra['retryPost'] != true) {
      return true;
    }

    return false;
  }
}
