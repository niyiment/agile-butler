
import 'dart:async';

/// Debounces function calls — only executes after [duration] has elapsed
/// since the last call. Ideal for search fields.
class Debouncer {
  final Duration duration;
  Timer? _timer;

  Debouncer({this.duration = const Duration(milliseconds: 500)});

  void run(void Function() action) {
    _timer?.cancel();
    _timer = Timer(duration, action);
  }

  void cancel() {
    _timer?.cancel();
    _timer = null;
  }

  bool get isActive => _timer?.isActive ?? false;

  void dispose() => cancel();
}

/// Throttle limits calls to at most once per [duration].
/// Unlike debounce, executes immediately and blocks subsequent calls.
class Throttler {
  final Duration duration;
  DateTime? _lastCall;

  Throttler({this.duration = const Duration(milliseconds: 1000)});

  bool call(void Function() action) {
    final now = DateTime.now();
    if (_lastCall == null || now.difference(_lastCall!) >= duration) {
      _lastCall = now;
      action();
      return true;
    }
    return false;
  }

  void reset() => _lastCall = null;
}
