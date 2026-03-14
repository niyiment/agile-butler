
import 'package:client/features/auth/data/models/user_model.dart';

class AuthResponse {
  final String accessToken;
  final String refreshToken;
  final String tokenType;
  final int expiresIn;
  final UserModel user;

  const AuthResponse({
    required this.accessToken,
    required this.refreshToken,
    required this.tokenType,
    required this.expiresIn,
    required this.user,
  });

  factory AuthResponse.fromJson(Map<String, dynamic> json) {
    final data = json['data'] as Map<String, dynamic>;

    return AuthResponse(
      accessToken: data['accessToken'] as String,
      refreshToken: data['refreshToken'] as String,
      tokenType: data['tokenType'] as String? ?? 'Bearer',
      expiresIn: data['expiresIn'] as int? ?? 3600,
      user: UserModel.fromJson(data['user'] as Map<String, dynamic>),
    );
  }
}
