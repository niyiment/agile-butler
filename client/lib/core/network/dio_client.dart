
import 'package:client/core/constants/app_config.dart';
import 'package:client/core/network/interceptors/auth_interceptor.dart';
import 'package:client/core/network/interceptors/error_interceptor.dart';
import 'package:client/core/network/interceptors/logging_interceptor.dart';
import 'package:client/core/network/interceptors/retry_interceptor.dart';
import 'package:dio/dio.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:logger/logger.dart';

class DioClient {
  static Dio create({
    required AppConfig config,
    required FlutterSecureStorage secureStorage,
    required Logger logger,
  }) {
    final dio = Dio(
      BaseOptions(
        baseUrl: config.fullBaseUrl,
        connectTimeout: config.connectTimeout,
        receiveTimeout: config.receiveTimeout,
        sendTimeout: config.sendTimeout,
        headers: {
          'Content-Type': 'application/json',
          'Accept': 'application/json',
          'X-App-Version': '1.0.0',
          'X-Platform': 'flutter',
        },
      ),
    );

    if (config.enableLogging) {
      dio.interceptors.add(
        LoggingInterceptor(
          logger: logger,
          logRequestBody: config.isDev,
          logResponseBody: config.isDev,
        ),
      );
    }

    dio.interceptors.add(ErrorInterceptor());

    dio.interceptors.add(
      AuthInterceptor(
        secureStorage: secureStorage,
        dio: dio,
        logger: logger,
      ),
    );

    dio.interceptors.add(
      RetryInterceptor(
        dio: dio,
        logger: logger,
        maxRetries: config.isDev ? 1 : 3,
      ),
    );

    return dio;
  }
}
