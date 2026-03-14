
import 'package:client/core/errors/exceptions.dart';
import 'package:client/core/errors/failures.dart';
import 'package:client/core/errors/result.dart';
import 'package:dio/dio.dart';
import 'package:logger/logger.dart';

/// Base class for all repositories.
abstract class BaseRepository {

  const BaseRepository({required this.logger});
  final Logger logger;

  Future<Result<T>> safeCall<T>(Future<T> Function() call) async {
    try {
      final data = await call();
      return Success(data);
    } on UnauthorizedException catch (e) {
      logger.w('Unauthorized: ${e.message}');
      return ResultFailure(UnauthorizedFailure(message: e.message));
    } on ForbiddenException catch (e) {
      logger.w('Forbidden: ${e.message}');
      return ResultFailure(ForbiddenFailure(message: e.message));
    } on NotFoundException catch (e) {
      logger.w('Not Found: ${e.message}');
      return ResultFailure(NotFoundFailure(message: e.message));
    } on TimeoutException catch (e) {
      logger.w('Timeout: ${e.message}');
      return ResultFailure(TimeoutFailure(message: e.message));
    } on ServerException catch (e) {
      logger.w('Server Error: ${e.message}');
      return ResultFailure(
        ServerFailure(
          message: e.message,
          statusCode: e.statusCode
        ),
      );
    } on CacheException catch (e) {
      logger.w('Cache error: ${e.message}');
      return ResultFailure(CacheFailure(message: e.message));
    } on RateLimitException catch (e) {
      logger.w('Rate limit exceeded: ${e.message}');
      return ResultFailure(
        RateLimitFailure(message: e.message, retryAfter: e.retryAfter)
      );
    } on DioException catch (e) {
      logger.e('Unhandled DioException', error: e);
      final appException = e.error;
      if (appException is AppException) {
        return ResultFailure(
          UnknownFailure(
            message: appException.message,
            code: appException.code,
          ),
        );
      }
      return ResultFailure(UnknownFailure(message: e.message ?? 'Request failed'));
    } catch (e, stack) {
      logger.e('Unexpected error in repository', error: e, stackTrace: stack);
      return ResultFailure(UnknownFailure(message: e.toString()));
    }
  }
}
