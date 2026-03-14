
import 'package:client/core/constants/storage_keys.dart';
import 'package:dio/dio.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:logger/logger.dart';


class AuthInterceptor extends Interceptor {

  AuthInterceptor({
    required FlutterSecureStorage secureStorage,
    required Dio dio,
    required Logger logger,
  })  : _secureStorage = secureStorage,
        _dio = dio,
        _logger = logger;
  final FlutterSecureStorage _secureStorage;
  final Dio _dio;
  final Logger _logger;

  bool _isRefreshing = false;
  final List<RequestOptions> _pendingRequests = [];

  @override
  Future<void> onRequest(
      RequestOptions options,
      RequestInterceptorHandler handler,
      ) async {
    if (options.extra['skipAuth'] == true) {
      return handler.next(options);
    }

    final token = await _secureStorage.read(key: StorageKeys.accessToken);
    if (token != null) {
      options.headers['Authorization'] = 'Bearer $token';
    }

    handler.next(options);
  }

  @override
  Future<void> onError(
      DioException err,
      ErrorInterceptorHandler handler,
      ) async {
    if (err.response?.statusCode == 401) {
      final refreshed = await _handleTokenRefresh(err.requestOptions);

      if (refreshed) {
        try {
          final token =
          await _secureStorage.read(key: StorageKeys.accessToken);
          final retryOptions = err.requestOptions;
          retryOptions.headers['Authorization'] = 'Bearer $token';

          final response = await _dio.fetch(retryOptions);
          return handler.resolve(response);
        } catch (e) {
          _logger.e('Retry after token refresh failed', error: e);
        }
      }

      await _clearTokens();
    }

    handler.next(err);
  }

  Future<bool> _handleTokenRefresh(RequestOptions originalRequest) async {
    if (_isRefreshing) {
      _pendingRequests.add(originalRequest);
      await Future.delayed(const Duration(seconds: 1));
      return _pendingRequests.contains(originalRequest) == false;
    }

    _isRefreshing = true;

    try {
      final refreshToken =
      await _secureStorage.read(key: StorageKeys.refreshToken);

      if (refreshToken == null) return false;

      final response = await _dio.post(
        ApiEndpoints.refreshToken,
        data: {'refresh_token': refreshToken},
        options: Options(extra: {'skipAuth': true}),
      );

      final newAccessToken = response.data['access_token'] as String?;
      final newRefreshToken = response.data['refresh_token'] as String?;

      if (newAccessToken == null) return false;

      await _secureStorage.write(
          key: StorageKeys.accessToken, value: newAccessToken);
      if (newRefreshToken != null) {
        await _secureStorage.write(
            key: StorageKeys.refreshToken, value: newRefreshToken);
      }

      _pendingRequests.clear();
      _isRefreshing = false;

      _logger.i('Token refreshed successfully');
      return true;
    } catch (e) {
      _isRefreshing = false;
      _pendingRequests.clear();
      _logger.e('Token refresh failed', error: e);
      return false;
    }
  }

  Future<void> _clearTokens() async {
    await _secureStorage.delete(key: StorageKeys.accessToken);
    await _secureStorage.delete(key: StorageKeys.refreshToken);
  }
}
