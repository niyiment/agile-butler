
import 'failures.dart';

sealed class Result<T> {
  const Result();

  bool get isSuccess => this is Success<T>;
  bool get isFailure => this is Failure;

  T? get dataOrNull => isSuccess ? (this as Success<T>).data : null;
  Failure? get failureOrNull => isFailure ? (this as ResultFailure<T>).failure : null;

  /// Execute [onSuccess] if result is [Success], [onFailure] if [ResultFailure].
  R when<R>({
    required R Function(T data) onSuccess,
    required R Function(Failure failure) onFailure,
  }) {
    return switch (this) {
      Success<T> s => onSuccess(s.data),
      ResultFailure<T> f => onFailure(f.failure),
    };
  }

  Result<R> map<R>(R Function(T data) transform) {
    return switch (this) {
      Success<T> s => Success(transform(s.data)),
      ResultFailure<T> f => ResultFailure(f.failure),
    };
  }

  Result<T> onSuccess(void Function(T data) action) {
    if (this case Success<T> s) action(s.data);
    return this;
  }

  Result<T> onFailure(void Function(Failure failure) action) {
    if (this case ResultFailure<T> f) action(f.failure);
    return this;
  }
}

final class Success<T> extends Result<T> {
  final T data;
  const Success(this.data);

  @override
  String toString() => 'Success(data: $data)';
}

final class ResultFailure<T> extends Result<T> {
  final Failure failure;
  const ResultFailure(this.failure);

  @override
  String toString() => 'ResultFailure(failure: $failure)';
}

extension ResultExtensions<T> on Result<T> {
  static Result<T> success<T>(T data) => Success(data);
  static Result<T> failure<T>(Failure failure) => ResultFailure(failure);
}
