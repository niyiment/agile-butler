
import 'package:equatable/equatable.dart';


abstract class Failure extends Equatable {
  final String message;
  final String? code;

  const Failure({
    required this.message,
    this.code,
  });

  @override
  List<Object?> get props => [message, code];
}

class NetworkFailure extends Failure {
  const NetworkFailure({
    super.message = 'No internet connection. Please check your network.',
    super.code,
  });
}

class ServerFailure extends Failure {
  final int? statusCode;

  const ServerFailure({
    super.message = 'Server error. Please try again later.',
    super.code,
    this.statusCode,
  });

  @override
  List<Object?> get props => [...super.props, statusCode];
}

class UnauthorizedFailure extends Failure {
  const UnauthorizedFailure({
    super.message = 'Session expired. Please login again.',
    super.code = '401',
  });
}

class ForbiddenFailure extends Failure {
  const ForbiddenFailure({
    super.message = 'You do not have permission to perform this action.',
    super.code = '403',
  });
}

class NotFoundFailure extends Failure {
  const NotFoundFailure({
    super.message = 'The requested resource was not found.',
    super.code = '404',
  });
}

class ValidationFailure extends Failure {
  final Map<String, List<String>>? fieldErrors;

  const ValidationFailure({
    super.message = 'Validation failed. Please check your input.',
    super.code = '422',
    this.fieldErrors,
  });

  @override
  List<Object?> get props => [...super.props, fieldErrors];
}

class CacheFailure extends Failure {
  const CacheFailure({
    super.message = 'Failed to load cached data.',
    super.code,
  });
}

class TimeoutFailure extends Failure {
  const TimeoutFailure({
    super.message = 'Request timed out. Please try again.',
    super.code,
  });
}

class UnknownFailure extends Failure {
  const UnknownFailure({
    super.message = 'An unexpected error occurred.',
    super.code,
  });
}

class RateLimitFailure extends Failure {
  final Duration? retryAfter;

  const RateLimitFailure({
    super.message = 'Too many requests. Please slow down.',
    super.code = '429',
    this.retryAfter,
  });

  @override
  List<Object?> get props => [...super.props, retryAfter];
}
