
class AppException implements Exception {
  final String message;
  final String? code;

  const AppException({required this.message, this.code});

  @override
  String toString() => 'AppException(message: $message, code: $code)';
}

class NetworkException extends AppException {
  const NetworkException({
    super.message = 'Network connection failed.',
    super.code,
  });
}

class ServerException extends AppException {
  final int statusCode;
  final Map<String, dynamic>? errorBody;

  const ServerException({
    required super.message,
    required this.statusCode,
    this.errorBody,
    super.code,
  });

  @override
  String toString() =>
      'ServerException(statusCode: $statusCode, message: $message)';
}

class UnauthorizedException extends AppException {
  const UnauthorizedException({
    super.message = 'Unauthorized. Token expired or invalid.',
    super.code = '401',
  });
}

class ForbiddenException extends AppException {
  const ForbiddenException({
    super.message = 'Access forbidden.',
    super.code = '403',
  });
}

class NotFoundException extends AppException {
  const NotFoundException({
    super.message = 'Resource not found.',
    super.code = '404',
  });
}

class ValidationException extends AppException {
  final Map<String, List<String>>? fieldErrors;

  const ValidationException({
    super.message = 'Validation failed.',
    super.code = '422',
    this.fieldErrors,
  });
}

class TimeoutException extends AppException {
  const TimeoutException({
    super.message = 'Connection timed out.',
    super.code,
  });
}

class CacheException extends AppException {
  const CacheException({
    super.message = 'Cache operation failed.',
    super.code,
  });
}

class RateLimitException extends AppException {
  final Duration? retryAfter;

  const RateLimitException({
    super.message = 'Rate limit exceeded.',
    super.code = '429',
    this.retryAfter,
  });
}
