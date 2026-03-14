
import 'package:equatable/equatable.dart';

/// Represents the authenticated user
class UserModel extends Equatable {
  final String id;
  final String name;
  final String email;
  final String? avatarUrl;
  final String role;
  final String timezone;
  final String? notificationTime;
  final String? teamId;
  final String? teamName;
  final DateTime createdAt;

  const UserModel({
    required this.id,
    required this.name,
    required this.email,
    this.avatarUrl,
    required this.role,
    required this.timezone,
    this.notificationTime,
    this.teamId,
    this.teamName,
    required this.createdAt,
  });

  factory UserModel.fromJson(Map<String, dynamic> json) {
    return UserModel(
      id: json['id'] as String,
      name: json['name'] as String,
      email: json['email'] as String,
      avatarUrl: json['avatarUrl'] as String?,
      role: json['role'] as String? ?? 'member',
      timezone: json['timezone'] as String? ?? 'UTC',
      notificationTime: json['notificationTime'] as String?,
      teamId: json['teamId'] as String?,
      teamName: json['teamName'] as String?,
      createdAt: DateTime.parse(json['createdAt'] as String),
    );
  }

  Map<String, dynamic> toJson() => {
    'id': id,
    'name': name,
    'email': email,
    'avatarUrl': avatarUrl,
    'role': role,
    'timezone': timezone,
    'notificationTime': notificationTime,
    'teamId': teamId,
    'teamName': teamName,
    'createdAt': createdAt.toIso8601String(),
  };

  UserModel copyWith({
    String? name,
    String? avatarUrl,
    String? timezone,
    String? notificationTime,
    String? teamId,
    String? teamName,
  }) {
    return UserModel(
      id: id,
      name: name ?? this.name,
      email: email,
      avatarUrl: avatarUrl ?? this.avatarUrl,
      role: role,
      timezone: timezone ?? this.timezone,
      notificationTime: notificationTime ?? this.notificationTime,
      teamId: teamId ?? this.teamId,
      teamName: teamName ?? this.teamName,
      createdAt: createdAt,
    );
  }

  String get initials {
    final parts = name.trim().split(' ');
    if (parts.length >= 2) {
      return '${parts.first[0]}${parts.last[0]}'.toUpperCase();
    }
    return name.isNotEmpty ? name[0].toUpperCase() : '?';
  }

  @override
  List<Object?> get props => [
    id,
    name,
    email,
    avatarUrl,
    role,
    timezone,
    notificationTime,
    teamId,
    teamName,
    createdAt,
  ];
}
