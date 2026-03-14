
import 'dart:convert';

import 'package:hive_flutter/hive_flutter.dart';
import 'package:logger/logger.dart';

import '../errors/exceptions.dart';

class CacheEntry<T> {

  CacheEntry({
    required this.data,
    required this.cachedAt,
    required this.ttl
  });
  final T data;
  final DateTime cachedAt;
  final Duration ttl;

  bool get isExpired => DateTime.now().isAfter(cachedAt.add(ttl));
  bool get isStale => DateTime.now().isAfter(cachedAt.add(ttl ~/2));
}

abstract class ICacheManager {
  Future<T?> get<T>(String key);
  Future<void> set<T>(String key, T data, {Duration? ttl});
  Future<void> remove(String key);
  Future<void> clear();
  bool containsKey(String key);
}

class MemoryCacheManager implements ICacheManager {

  MemoryCacheManager({this.maxSize = 100});
  final Map<String, CacheEntry<dynamic>> _cache = {};
  final int maxSize;

  @override
  Future<T?> get<T>(String key) async {
    final entry = _cache[key];
    if (entry == null) return null;
    if (entry.isExpired) {
      _cache.remove(key);
      return null;
    }
    return entry.data as T?;
  }

  @override
  Future<void> set<T>(
      String key,
      T data, {
        Duration? ttl,
      }) async {
    if (_cache.length >= maxSize && !_cache.containsKey(key)) {
      final oldestKey = _cache.entries
          .reduce((a, b) =>
      a.value.cachedAt.isBefore(b.value.cachedAt) ? a : b)
          .key;
      _cache.remove(oldestKey);
    }

    _cache[key] = CacheEntry(
      data: data,
      cachedAt: DateTime.now(),
      ttl: ttl ?? const Duration(minutes: 5),
    );
  }

  @override
  Future<void> remove(String key) async => _cache.remove(key);

  @override
  Future<void> clear() async => _cache.clear();

  @override
  bool containsKey(String key) {
    final entry = _cache[key];
    if (entry == null) return false;
    if (entry.isExpired) {
      _cache.remove(key);
      return false;
    }
    return true;
  }

  CacheEntry<dynamic>? getEntry(String key) => _cache[key];
}

/// disk cache – persistent, survives app restart
class DiskCacheManager implements ICacheManager {

  DiskCacheManager({required Logger logger}) : _logger = logger;
  static const String _boxName = 'app_cache';
  static const String _metaBoxName = 'app_cache_meta';

  late Box<String> _box;
  late Box<Map> _metaBox;
  final Logger _logger;

  Future<void> init() async {
    _box = await Hive.openBox<String>(_boxName);
    _metaBox = await Hive.openBox<Map>(_metaBoxName);
    _logger.i('DiskCacheManager initialized');
  }

  @override
  Future<T?> get<T>(String key) async {
    try {
      final meta = _metaBox.get(key);
      if (meta == null) return null;

      final cachedAt = DateTime.parse(meta['cachedAt'] as String);
      final ttlMs = meta['ttlMs'] as int;
      final expiry = cachedAt.add(Duration(milliseconds: ttlMs));

      if (DateTime.now().isAfter(expiry)) {
        await remove(key);
        return null;
      }

      final jsonStr = _box.get(key);
      if (jsonStr == null) return null;

      final decoded = jsonDecode(jsonStr);
      return decoded as T?;
    } catch (e) {
      _logger.w('DiskCache get failed for key: $key', error: e);
      return null;
    }
  }

  @override
  Future<void> set<T>(
      String key,
      T data, {
        Duration? ttl,
      }) async {
    try {
      final effectiveTtl = ttl ?? const Duration(hours: 1);
      await _box.put(key, jsonEncode(data));
      await _metaBox.put(key, {
        'cachedAt': DateTime.now().toIso8601String(),
        'ttlMs': effectiveTtl.inMilliseconds,
      });
    } catch (e) {
      _logger.w('DiskCache set failed for key: $key', error: e);
      throw const CacheException(message: 'Failed to write to disk cache.');
    }
  }

  @override
  Future<void> remove(String key) async {
    await _box.delete(key);
    await _metaBox.delete(key);
  }

  @override
  Future<void> clear() async {
    await _box.clear();
    await _metaBox.clear();
  }

  @override
  bool containsKey(String key) => _box.containsKey(key);
}

/// Composite cache manager implementing stale-while-revalidate
class AppCacheManager {

  AppCacheManager({
    required MemoryCacheManager l1,
    required DiskCacheManager l2,
  })  : _l1 = l1,
        _l2 = l2;
  final MemoryCacheManager _l1;
  final DiskCacheManager _l2;

  /// Get data from cache. Returns [CacheResult] with data and freshness info.
  Future<CacheResult<T>> get<T>(String key) async {
    // Try L1 (memory) first
    final l1Entry = _l1.getEntry(key);
    if (l1Entry != null && !l1Entry.isExpired) {
      return CacheResult(
        data: l1Entry.data as T,
        isStale: l1Entry.isStale,
        source: CacheSource.memory,
      );
    }

    // Try L2 (disk)
    final l2Data = await _l2.get<T>(key);
    if (l2Data != null) {
      // Promote to L1
      await _l1.set(key, l2Data);
      return CacheResult(
        data: l2Data,
        isStale: false, // Disk cache handles its own staleness
        source: CacheSource.disk,
      );
    }

    return CacheResult(data: null, isStale: true, source: CacheSource.none);
  }

  Future<void> set<T>(
      String key,
      T data, {
        Duration? memoryTtl,
        Duration? diskTtl,
      }) async {
    await _l1.set(key, data, ttl: memoryTtl);
    await _l2.set(key, data, ttl: diskTtl);
  }

  Future<void> invalidate(String key) async {
    await _l1.remove(key);
    await _l2.remove(key);
  }

  Future<void> invalidateAll() async {
    await _l1.clear();
    await _l2.clear();
  }
}

enum CacheSource { memory, disk, none }

class CacheResult<T> {

  const CacheResult({
    required this.data,
    required this.isStale,
    required this.source,
  });
  final T? data;
  final bool isStale;
  final CacheSource source;

  bool get hasData => data != null;
}

