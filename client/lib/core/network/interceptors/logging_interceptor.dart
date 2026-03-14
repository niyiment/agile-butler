
import 'package:dio/dio.dart';
import 'package:logger/logger.dart';

class LoggingInterceptor extends Interceptor {

  LoggingInterceptor({
    required Logger logger,
    this.logRequestBody = true,
    this.logResponseBody = true,
  }) : _logger = logger;
  final Logger _logger;
  final bool logRequestBody;
  final bool logResponseBody;

  @override
  void onRequest(RequestOptions options, RequestInterceptorHandler handler) {
    final buffer = StringBuffer()
      ..writeln('│ REQUEST')
      ..writeln('│ ${options.method} ${options.uri}')
      ..writeln('│ Headers: ${_sanitizeHeaders(options.headers)}');

    if (logRequestBody && options.data != null) {
      buffer.writeln('│ Body: ${options.data}');
    }

    _logger.d(buffer.toString());
    handler.next(options);
  }

  @override
  void onResponse(Response response, ResponseInterceptorHandler handler) {
    final buffer = StringBuffer()
      ..writeln('│ RESPONSE [${response.statusCode}]')
      ..writeln('│ ${response.requestOptions.method} ${response.requestOptions.uri}')
      ..writeln('│ Duration: ${_getDuration(response.requestOptions)}ms');

    if (logResponseBody) {
      buffer.writeln('│ Body: ${response.data}');
    }

    _logger.i(buffer.toString());
    handler.next(response);
  }

  @override
  void onError(DioException err, ErrorInterceptorHandler handler) {
    final buffer = StringBuffer()
      ..writeln('│ ERROR [${err.response?.statusCode ?? 'NO STATUS'}]')
      ..writeln('│ ${err.requestOptions.method} ${err.requestOptions.uri}')
      ..writeln('│ Type: ${err.type}')
      ..writeln('│ Message: ${err.message}');

    if (err.response?.data != null) {
      buffer.writeln('│ Response: ${err.response?.data}');
    }

    _logger.e(buffer.toString(), error: err);
    handler.next(err);
  }

  Map<String, dynamic> _sanitizeHeaders(Map<String, dynamic> headers) {
    final sanitized = Map<String, dynamic>.from(headers)
    ..remove('Authorization')
    ..remove('X-API-Key');
    return sanitized;
  }

  String _getDuration(RequestOptions options) {
    final startTime = options.extra['startTime'] as DateTime?;
    if (startTime == null) return 'unknown';
    return DateTime.now().difference(startTime).inMilliseconds.toString();
  }
}
