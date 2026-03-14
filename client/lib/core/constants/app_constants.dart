abstract class AppConstants {
  static const String baseUrl = String.fromEnvironment(
    'BASE_URL',
    defaultValue: 'https://localhost:8080/api/v1',
  );

  static const String environment = String.fromEnvironment(
    'ENV',
    defaultValue: 'development',
  );

  static const bool isProd = environment == 'production';
  static const bool isStaging = environment == 'staging';
  static const bool isDev = environment == 'development';

  // Pagination defaults
  static const int defaultPageSize = 20;
  static const int prefetchThreshold = 5;

  // Cache TTLs
  static const Duration shortCacheTtl = Duration(minutes: 5);
  static const Duration mediumCacheTtl = Duration(minutes: 30);
  static const Duration longCacheTtl = Duration(hours: 24);

  static const kTimezones = [
  'UTC',
  'America/New_York',
  'America/Chicago',
  'America/Denver',
  'America/Los_Angeles',
  'America/Sao_Paulo',
  'Europe/London',
  'Europe/Paris',
  'Europe/Berlin',
  'Europe/Moscow',
  'Africa/Lagos',
  'Africa/Nairobi',
  'Africa/Johannesburg',
  'Asia/Dubai',
  'Asia/Kolkata',
  'Asia/Singapore',
  'Asia/Tokyo',
  'Australia/Sydney',
  'Pacific/Auckland',
  ];
}
