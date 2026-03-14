import 'package:client/features/auth/data/models/user_model.dart';
import 'package:client/features/auth/data/repositories/auth_repository.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:client/core/errors/failures.dart';
import 'package:equatable/equatable.dart';

part 'auth_event.dart';
part 'auth_state.dart';


class AuthBloc extends Bloc<AuthEvent, AuthState> {
  final  IAuthRepository _authRepository;

  AuthBloc({required IAuthRepository authRepository})
      : _authRepository = authRepository,
        super(const AuthState()) {
    on<AuthCheckRequested>(_onCheckRequested);
    on<AuthLoginRequested>(_onLoginRequested);
    on<AuthRegisterRequested>(_onRegisterRequested);
    on<AuthLogoutRequested>(_onLogoutRequested);
  }

  Future<void> _onCheckRequested(
      AuthCheckRequested event,
      Emitter<AuthState> emit,
      ) async {
    final isAuth = await _authRepository.isAuthenticated();
    if (!isAuth) {
      emit(state.copyWith(status: AuthStatus.unauthenticated));
      return;
    }

    final cachedUser = await _authRepository.getCachedUser();
    emit(state.copyWith(
      status: AuthStatus.authenticated,
      user: cachedUser,
    ));
  }

  Future<void> _onLoginRequested(
      AuthLoginRequested event,
      Emitter<AuthState> emit,
      ) async {
    emit(state.copyWith(status: AuthStatus.loading, clearFailure: true));

    final result = await _authRepository.login(
      email: event.email,
      password: event.password,
    );

    result.when(
      onSuccess: (response) => emit(state.copyWith(
        status: AuthStatus.authenticated,
        user: response.user,
        clearFailure: true,
      )),
      onFailure: (failure) => emit(state.copyWith(
        status: AuthStatus.unauthenticated,
        failure: failure,
      )),
    );
  }

  Future<void> _onRegisterRequested(
      AuthRegisterRequested event,
      Emitter<AuthState> emit,
      ) async {
    emit(state.copyWith(status: AuthStatus.loading, clearFailure: true));

    final result = await _authRepository.register(
      name: event.name,
      email: event.email,
      password: event.password,
      timezone: event.timezone,
    );

    result.when(
      onSuccess: (response) => emit(state.copyWith(
        status: AuthStatus.authenticated,
        user: response.user,
        clearFailure: true,
      )),
      onFailure: (failure) => emit(state.copyWith(
        status: AuthStatus.unauthenticated,
        failure: failure,
      )),
    );
  }

  Future<void> _onLogoutRequested(
      AuthLogoutRequested event,
      Emitter<AuthState> emit,
      ) async {
    emit(state.copyWith(status: AuthStatus.loading));
    await _authRepository.logout();
    emit(const AuthState(status: AuthStatus.unauthenticated));
  }
}
