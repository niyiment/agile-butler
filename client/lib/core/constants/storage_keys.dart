
abstract class StorageKeys {
  // Auth
  static const String accessToken = 'access_token';
  static const String refreshToken = 'refresh_token';
  static const String userId = 'user_id';
  static const String cachedUser = 'cached_user';

  // App State
  static const String onboardingCompleted = 'onboarding_completed';
  static const String selectedTheme = 'selected_theme';
  static const String selectedLocale = 'selected_locale';

  // Cache keys prefix
  static const String cachePrefix = 'cache_';
  static const String userProfile = '${cachePrefix}user_profile';
}

abstract class ApiEndpoints {
  // Auth
  static const String login = '/auth/login';
  static const String register = '/auth/register';
  static const String logout = '/auth/logout';
  static const String refreshToken = '/auth/refresh';
  static const String forgotPassword = '/auth/forgot-password';
  static const String resetPassword = '/auth/reset-password';

  // User
  static const String profile = '/user/profile';
  static const String updateProfile = '/user/profile';

  // Posts (example feature)
  static const String posts = '/posts';
  static String postById(String id) => '/posts/$id';
  static String postComments(String id) => '/posts/$id/comments';
}