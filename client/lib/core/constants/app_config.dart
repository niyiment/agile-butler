enum Environment { dev, staging, production }

class AppConfig {
  final Environment environment;
  final String baseUrl;
  final String apiVersion;
  final bool enableLogging;
  final bool enableCertificatePinning;
  final Duration connectTimeout;
  final Duration receiveTimeout;
  final Duration sendTimeout;

  AppConfig({
    required this.environment,
    required this.baseUrl,
    required this.apiVersion,
    required this.enableLogging,
    required this.enableCertificatePinning,
     this.connectTimeout = const Duration(seconds: 30),
     this.receiveTimeout = const Duration(seconds: 30),
     this.sendTimeout = const Duration(seconds: 30),
  });

  static final dev = AppConfig(
    environment: Environment.dev,
    baseUrl: 'http://10.0.2.2:8080/api',
    apiVersion: 'v1',
    enableLogging: true,
    enableCertificatePinning: false,
  );

  static final staging = AppConfig(
    environment: Environment.staging,
    baseUrl: 'https://api.yourdomain.com/api',
    apiVersion: 'v1',
    enableLogging: true,
    enableCertificatePinning: true,
  );

  static final production = AppConfig(
    environment: Environment.production,
    baseUrl: 'https://api.yourdomain.com/api',
    apiVersion: 'v1',
    enableLogging: false,
    enableCertificatePinning: true,
  );

  String get fullBaseUrl => '$baseUrl/$apiVersion';

  bool get isDev => environment == Environment.dev;
  bool get isProduction => environment == Environment.production;
}
