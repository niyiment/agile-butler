
import 'package:client/core/cache/cache_manager.dart';
import 'package:client/core/constants/app_config.dart';
import 'package:client/core/network/dio_client.dart';
import 'package:client/core/services/connectivity_service.dart';
import 'package:client/core/utils/request_deduplicator.dart';
import 'package:client/features/auth/data/datasources/auth_remote_datasource.dart';
import 'package:client/features/auth/data/repositories/auth_repository.dart';
import 'package:client/features/auth/presentation/bloc/auth_bloc.dart';
import 'package:dio/dio.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:get_it/get_it.dart';
import 'package:hive_flutter/hive_flutter.dart';
import 'package:logger/logger.dart';


final getIt = GetIt.instance;

Future<void> configureDependencies({required AppConfig config}) async {
  await _initHive();
  _registerCore(config);
  _registerNetwork(config);
  _registerCache();
  _registerDataSources();
  _registerRepositories();
  _registerBlocs();
}

Future<void> _initHive() async {
  await Hive.initFlutter();
}

void _registerCore(AppConfig config) {
  // Config
  getIt..registerSingleton<AppConfig>(config)

  // Logger
  ..registerSingleton<Logger>(
    Logger(
      printer: PrettyPrinter(
        methodCount: 2,
        errorMethodCount: 8,
        lineLength: 120,
        colors: true,
        printEmojis: true,
      ),
      level: config.isDev ? Level.debug : Level.warning,
    ),
  )

  // Secure storage
  ..registerSingleton<FlutterSecureStorage>(
    const FlutterSecureStorage(
      aOptions: AndroidOptions(),
      iOptions: IOSOptions(accessibility: KeychainAccessibility.first_unlock),
    ),
  )

  ..registerSingleton<ConnectivityService>(ConnectivityService());
}

void _registerNetwork(AppConfig config) {
  getIt.registerSingleton<Dio>(
    DioClient.create(
      config: config,
      secureStorage: getIt<FlutterSecureStorage>(),
      logger: getIt<Logger>(),
    ),
  );
}

void _registerCache() {
  final memoryCache = MemoryCacheManager(maxSize: 200);
  final diskCache = DiskCacheManager(logger: getIt<Logger>())

  ..init();

  getIt..registerSingleton<MemoryCacheManager>(memoryCache)
  ..registerSingleton<DiskCacheManager>(diskCache)
  ..registerSingleton<AppCacheManager>(
    AppCacheManager(l1: memoryCache, l2: diskCache),
  )
  ..registerFactory<RequestDeduplicator>(RequestDeduplicator.new);
}

void _registerDataSources() {
  getIt.registerLazySingleton<IAuthRemoteDataSource>(
        () => AuthRemoteDataSource(getIt<Dio>()),
  );
}

void _registerRepositories() {
  getIt.registerLazySingleton<IAuthRepository>(
        () => AuthRepository(
      remoteDataSource: getIt<IAuthRemoteDataSource>(),
      storage: getIt<FlutterSecureStorage>(),
      logger: getIt<Logger>(),
    ),
  );
}

void _registerBlocs() {
  getIt.registerSingleton<AuthBloc>(
    AuthBloc(
      authRepository: getIt<IAuthRepository>(),
    ),
  );
}
Future<void> resetDependencies() async {
  await getIt.reset();
}
