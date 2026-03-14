
import 'dart:convert';

import 'package:client/core/constants/storage_keys.dart';
import 'package:client/core/errors/result.dart';
import 'package:client/data/repositories/base_Repository.dart';
import 'package:client/features/auth/data/models/auth_response.dart';
import 'package:client/features/auth/data/models/user_model.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:logger/logger.dart';

import '../datasources/auth_remote_datasource.dart';

abstract class IAuthRepository {
  Future<Result<AuthResponse>> login({
    required String email,
    required String password,
  });

  Future<Result<AuthResponse>> register({
    required String name,
    required String email,
    required String password,
    required String timezone,
  });

  Future<void> logout();

  Future<Result<AuthResponse>> refreshToken({
    required String refreshToken,
  });

  Future<bool> isAuthenticated();

  Future<UserModel?> getCachedUser();
}

class AuthRepository extends BaseRepository implements IAuthRepository {
  final IAuthRemoteDataSource _remoteDataSource;
  final FlutterSecureStorage _storage;

  AuthRepository({
    required IAuthRemoteDataSource remoteDataSource,
    required FlutterSecureStorage storage,
    required Logger logger
}) : _remoteDataSource = remoteDataSource,
  _storage = storage,
  super(logger: logger);

  @override
  Future<UserModel?> getCachedUser() async {
    try {
      final raw = await _storage.read(key: StorageKeys.cachedUser);
      if (raw == null) {
        return null;
      }
      return UserModel.fromJson(jsonDecode(raw) as Map<String, dynamic>);
    } catch (e) {
      logger.w('Failed to read cache user', error: e);
      return null;
    }
  }

  @override
  Future<bool> isAuthenticated() async {
    final token = await _storage.read(key: StorageKeys.accessToken);
    return token != null && token.isNotEmpty;
  }

  @override
  Future<Result<AuthResponse>> login({required String email, required String password}) async {
    return safeCall(() async {
      final response = await _remoteDataSource.login(email: email, password: password);
      await _persistSession(response);
      return response;
    });
  }

  @override
  Future<void> logout() async {
    try {
      await _remoteDataSource.logout();
    } catch(e) {
      logger.w('Logout API call failed. Clearing local session.');
    } finally {
      await _clearSession();
    }
  }

  @override
  Future<Result<AuthResponse>> refreshToken({required String refreshToken}) {
    return safeCall(() async {
      final token = await _storage.read(key: StorageKeys.refreshToken);
      if (token == null) {
        throw Exception('No refresh token found');
      }
      final response = await _remoteDataSource.refreshToken(refreshToken: refreshToken);
      await _persistSession(response);
      return response;
    });
  }

  @override
  Future<Result<AuthResponse>> register({required String name, required String email, required String password, required String timezone}) {
    return safeCall(() async {
      final response = await _remoteDataSource.register(
          name: name, email: email, password: password, timezone: timezone);
      await _persistSession(response);
      return response;
    });
  }

  Future<void> _persistSession(AuthResponse response) async {
    await Future.wait([
      _storage.write(key: StorageKeys.accessToken, value: response.accessToken),
      _storage.write(key: StorageKeys.refreshToken, value: response.refreshToken),
      _storage.write(key: StorageKeys.userId, value: response.user.id),
      _storage.write(key: StorageKeys.cachedUser, value: jsonEncode(response.user.toJson())),
    ]);
    logger.i('Session persisted for user: ${response.user.email}');
  }

  Future<void> _clearSession() async {
    await _storage.deleteAll();
    logger.i('Session cleared');
  }

}
