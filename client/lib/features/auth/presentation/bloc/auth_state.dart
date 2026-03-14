part of 'auth_bloc.dart';

enum AuthStatus { initial, authenticated, unauthenticated, loading }

class AuthState extends Equatable {
  final AuthStatus status;
  final UserModel? user;
  final Failure? failure;

  const AuthState({
    this.status = AuthStatus.initial,
    this.user,
    this.failure,
  });

  bool get isAuthenticated => status == AuthStatus.authenticated;
  bool get isLoading => status == AuthStatus.loading;
  bool get hasError => failure != null;

  AuthState copyWith({
    AuthStatus? status,
    UserModel? user,
    Failure? failure,
    bool clearFailure = false,
    bool clearUser = false,
  }) {
    return AuthState(
      status: status ?? this.status,
      user: clearUser ? null : (user ?? this.user),
      failure: clearFailure ? null : (failure ?? this.failure),
    );
  }

  @override
  List<Object?> get props => [status, user, failure];
}
