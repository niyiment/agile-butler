
import 'package:dio/dio.dart';

import '../../errors/exceptions.dart';

class ErrorInterceptor extends Interceptor {
  @override
  void onError(DioException err, ErrorInterceptorHandler handler) {
    final transformedError = _transformError(err);
    handler.next(transformedError);
  }

  DioException _transformError(DioException err) {
    final exception = switch (err.type) {
      DioExceptionType.connectionTimeout ||
      DioExceptionType.receiveTimeout ||
      DioExceptionType.sendTimeout =>
      const TimeoutException(),
      DioExceptionType.connectionError => const NetworkException(),
      DioExceptionType.cancel => const AppException(
        message: 'Request was cancelled.',
        code: 'CANCELLED',
      ),
      DioExceptionType.badResponse => _parseServerError(err),
      _ => AppException(
        message: err.message ?? 'An unexpected error occurred.',
      ),
    };

    return err.copyWith(
      error: exception,
      message: exception.message,
    );
  }

  AppException _parseServerError(DioException err) {
    final statusCode = err.response?.statusCode;
    final data = err.response?.data;
    final message = _extractMessage(data);

    return switch (statusCode) {
      401 => const UnauthorizedException(),
      403 => const ForbiddenException(),
      404 => const NotFoundException(),
      422 => ValidationException(
        message: message ?? 'Validation failed.',
        fieldErrors: _extractFieldErrors(data),
      ),
      429 => RateLimitException(
        retryAfter: _extractRetryAfter(err.response?.headers),
      ),
      _ when statusCode != null && statusCode >= 500 => ServerException(
        message: message ?? 'Server error. Please try again.',
        statusCode: statusCode,
        errorBody: data is Map<String, dynamic> ? data : null,
      ),
      _ => ServerException(
        message: message ?? 'Request failed.',
        statusCode: statusCode ?? 0,
      ),
    };
  }

  String? _extractMessage(dynamic data) {
    if (data is Map<String, dynamic>) {
      return data['message'] as String? ??
          data['error'] as String? ??
          data['detail'] as String?;
    }
    return null;
  }

  Map<String, List<String>>? _extractFieldErrors(dynamic data) {
    if (data is! Map<String, dynamic>) return null;
    final errors = data['errors'];
    if (errors is! Map) return null;

    return errors.map((key, value) {
      final messages = value is List
          ? value.map((e) => e.toString()).toList()
          : [value.toString()];
      return MapEntry(key.toString(), messages);
    });
  }

  Duration? _extractRetryAfter(Headers? headers) {
    final retryAfter = headers?.value('retry-after');
    if (retryAfter == null) return null;
    final seconds = int.tryParse(retryAfter);
    return seconds != null ? Duration(seconds: seconds) : null;
  }
}
