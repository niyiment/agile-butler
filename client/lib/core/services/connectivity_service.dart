

import 'dart:async';

import 'package:connectivity_plus/connectivity_plus.dart';
import 'package:injectable/injectable.dart';

/// Service for checking network connectivity
@singleton
class ConnectivityService {
  final Connectivity _connectivity;
  late StreamSubscription<List<ConnectivityResult>> _subscription;
  final StreamController<bool> _controller = StreamController<bool>.broadcast();
  bool _isConnected = true;

  ConnectivityService({Connectivity? connectivity})
    : _connectivity = connectivity ?? Connectivity() {
    _init();
  }

  Future<void> _init() async {
    final results = await _connectivity.checkConnectivity();
    _isConnected = _hasConnection(results);

  }

  bool _hasConnection(List<ConnectivityResult> results) {
    return results.any((r) =>
        r == ConnectivityResult.wifi ||
        r == ConnectivityResult.mobile ||
        r == ConnectivityResult.ethernet
    );
  }
}
