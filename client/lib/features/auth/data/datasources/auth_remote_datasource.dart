import 'package:client/core/constants/storage_keys.dart';
import 'package:client/features/auth/data/models/auth_response.dart';
import 'package:dio/dio.dart';


/// Remote data source for authentication
abstract class IAuthRemoteDataSource {
  Future<AuthResponse> login({
    required String email,
    required String password,
  });

  Future<AuthResponse> register({
    required String name,
    required String email,
    required String password,
    required String timezone,
  });

  Future<AuthResponse> refreshToken({
    required String refreshToken,
  });

  Future<void> logout();
}

class AuthRemoteDataSource implements IAuthRemoteDataSource {

  AuthRemoteDataSource(this._dio);
  final Dio _dio;

  @override
  Future<AuthResponse> login(
      {required String email, required String password}) async {
    final response = await _dio.post(
        ApiEndpoints.login,
        data: {
          'email': email,
          'password': password,
        },
        options: Options(extra: {'skipAuth': true})
    );

    return AuthResponse.fromJson(response.data as Map<String, dynamic>);
  }

  @override
  Future<void> logout() async {
    await _dio.post(ApiEndpoints.logout);
  }

  @override
  Future<AuthResponse> refreshToken({required String refreshToken}) async {
    final response = await _dio.post(
        ApiEndpoints.refreshToken,
        data: {
          'refreshToken': refreshToken,
        },
        options: Options(extra: {'skipAuth': true})
    );

    return AuthResponse.fromJson(response.data as Map<String, dynamic>);
  }

  @override
  Future<AuthResponse> register(
      {required String name, required String email, required String password, required String timezone}) async {

    final response = await _dio.post(
        ApiEndpoints.register,
       data: {
          'name': name,
          'email': email,
          'password': password,
          'timezone': timezone,
       },
        options: Options(extra: {'skipAuth': true})
    );
    return AuthResponse.fromJson(response.data as Map<String, dynamic>);
  }

}
