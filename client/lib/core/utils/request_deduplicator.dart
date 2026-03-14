
/// Prevents duplicate in-flight request for the same key
class RequestDeduplicator<T> {
  final Map<String, Future<T>> _inFlight = {};

  Future<T> deduplicate(String key, Future<T> Function() factory) {
    if (_inFlight.containsKey(key)) {
      return _inFlight[key]!;
    }

    final future = factory().whenComplete(() => _inFlight.remove(key));
    _inFlight[key] = future;
    return future;
  }

  bool isInFlight(String key) => _inFlight.containsKey(key);
  void cancel(String key) => _inFlight.remove(key);
  void cancelAll() => _inFlight.clear();
}
