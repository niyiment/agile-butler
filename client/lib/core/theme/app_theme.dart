
// lib/core/theme/app_theme.dart
//
// Single source of truth for the Agile Butler brand.
//
// Brand spec:
//   Primary   → #00D6AB  (mint teal)
//   Background → #0F231F  (deep forest dark)
//   Font       → Manrope (Google Fonts)

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:google_fonts/google_fonts.dart';

abstract class AppTheme {
  static const Color primary = Color(0xFF00D6AB);
  static const Color bgDark = Color(0xFF0F231F);
  static const Color cardDark = Color(0x1A00D6AB);
  static const Color borderDark = Color(0x2600D6AB);
  static Color primarySubtle(double opacity) => primary.withValues(alpha: opacity);

  //  Typography
  static TextTheme _manrope(TextTheme base) =>
      GoogleFonts.manropeTextTheme(base);

  //  ColorScheme factories
  static ColorScheme _colorScheme() {
    return ColorScheme(
      brightness: Brightness.light,
      primary: primary,
      onPrimary: Colors.white,
      primaryContainer: primary.withValues(alpha: 0.12),
      onPrimaryContainer: const Color(0xFF004D3A),
      secondary: const Color(0xFF00B894),
      onSecondary: Colors.white,
      secondaryContainer: const Color(0xFF00B894).withValues(alpha: 0.10),
      onSecondaryContainer: const Color(0xFF003D30),
      tertiary: const Color(0xFF00CEC9),
      onTertiary: Colors.white,
      tertiaryContainer: const Color(0xFF00CEC9).withValues(alpha: 0.10),
      onTertiaryContainer: const Color(0xFF003D3C),
      error: const Color(0xFFDC2626),
      onError: Colors.white,
      errorContainer: const Color(0xFFDC2626).withValues(alpha: 0.10),
      onErrorContainer: const Color(0xFF7F1D1D),
      surface: bgDark,
      onSurface: const Color(0xFF0F172A),
      surfaceContainerHighest: const Color(0xFFECFDF5),
      onSurfaceVariant: const Color(0xFF475569),
      outline: const Color(0xFFCBD5E1),
      outlineVariant: const Color(0xFFE2E8F0),
      inverseSurface: bgDark,
      onInverseSurface: const Color(0xFFF1F5F9),
      inversePrimary: primary,
      scrim: Colors.black,
      shadow: Colors.black,
    );
  }

  static ThemeData light() {
    final cs = _colorScheme();
    final base = ThemeData.light(useMaterial3: true);

    return base.copyWith(
      colorScheme: cs,
      scaffoldBackgroundColor: bgDark,
      textTheme: _manrope(base.textTheme),
      primaryTextTheme: _manrope(base.primaryTextTheme),

      appBarTheme: AppBarTheme(
        backgroundColor: bgDark,
        foregroundColor: const Color(0xFF0F172A),
        elevation: 0,
        scrolledUnderElevation: 0,
        systemOverlayStyle: const SystemUiOverlayStyle(
          statusBarColor: Colors.transparent,
          statusBarIconBrightness: Brightness.dark,
        ),
        titleTextStyle: GoogleFonts.manrope(
          color: const Color(0xFF0F172A),
          fontSize: 18,
          fontWeight: FontWeight.w700,
        ),
        iconTheme: const IconThemeData(color: Color(0xFF0F172A)),
      ),

      inputDecorationTheme: InputDecorationTheme(
        filled: true,
        fillColor: const Color(0xFFF8FAFC),
        contentPadding:
        const EdgeInsets.symmetric(horizontal: 16, vertical: 16),
        hintStyle: const TextStyle(color: Color(0xFF94A3B8)),
        labelStyle: const TextStyle(color: Color(0xFF475569)),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: const BorderSide(color: Color(0xFFCBD5E1)),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: const BorderSide(color: primary, width: 1.5),
        ),
        errorBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: const BorderSide(color: Color(0xFFDC2626)),
        ),
        focusedErrorBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: const BorderSide(color: Color(0xFFDC2626), width: 1.5),
        ),
        errorStyle: const TextStyle(color: Color(0xFFDC2626), fontSize: 12),
      ),

      elevatedButtonTheme: ElevatedButtonThemeData(
        style: ElevatedButton.styleFrom(
          backgroundColor: primary,
          foregroundColor: Colors.white,
          minimumSize: const Size(double.infinity, 52),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(12),
          ),
          elevation: 0,
          textStyle: GoogleFonts.manrope(
            fontWeight: FontWeight.w800,
            fontSize: 15,
          ),
        ),
      ),

      filledButtonTheme: FilledButtonThemeData(
        style: FilledButton.styleFrom(
          backgroundColor: primary,
          foregroundColor: Colors.white,
          minimumSize: const Size(double.infinity, 52),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(12),
          ),
          textStyle: GoogleFonts.manrope(
            fontWeight: FontWeight.w800,
            fontSize: 15,
          ),
        ),
      ),

      outlinedButtonTheme: OutlinedButtonThemeData(
        style: OutlinedButton.styleFrom(
          foregroundColor: primary,
          side: const BorderSide(color: primary),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(12),
          ),
          textStyle: GoogleFonts.manrope(fontWeight: FontWeight.w700),
        ),
      ),

      textButtonTheme: TextButtonThemeData(
        style: TextButton.styleFrom(
          foregroundColor: primary,
          textStyle: GoogleFonts.manrope(fontWeight: FontWeight.w700),
        ),
      ),

      cardTheme: CardThemeData(
        color: cardDark,
        elevation: 0,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(16),
          side: const BorderSide(color: Color(0xFFE2E8F0)),
        ),
        margin: const EdgeInsets.symmetric(vertical: 6),
      ),

      navigationBarTheme: NavigationBarThemeData(
        backgroundColor: cardDark,
        indicatorColor: primary.withValues(alpha: 0.12),
        iconTheme: WidgetStateProperty.resolveWith((states) {
          if (states.contains(WidgetState.selected)) {
            return const IconThemeData(color: primary);
          }
          return const IconThemeData(color: Color(0xFF64748B));
        }),
        labelTextStyle: WidgetStateProperty.resolveWith((states) {
          final selected = states.contains(WidgetState.selected);
          return GoogleFonts.manrope(
            fontSize: 10,
            fontWeight: selected ? FontWeight.w700 : FontWeight.w500,
            color: selected ? primary : const Color(0xFF64748B),
          );
        }),
        surfaceTintColor: Colors.transparent,
      ),

      chipTheme: ChipThemeData(
        backgroundColor: primary.withValues(alpha: 0.08),
        selectedColor: primary.withValues(alpha: 0.15),
        labelStyle: GoogleFonts.manrope(fontSize: 12, fontWeight: FontWeight.w600),
        side: BorderSide(color: primary.withValues(alpha: 0.2)),
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(999),
        ),
      ),

      dividerTheme: const DividerThemeData(
        color: Color(0xFFE2E8F0),
        thickness: 1,
      ),

      snackBarTheme: SnackBarThemeData(
        backgroundColor: const Color(0xFF1E293B),
        contentTextStyle: GoogleFonts.manrope(
          color: const Color(0xFFF1F5F9),
          fontSize: 14,
        ),
        actionTextColor: primary,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(12),
        ),
        behavior: SnackBarBehavior.floating,
      ),

      progressIndicatorTheme: const ProgressIndicatorThemeData(
        color: primary,
      ),

      switchTheme: SwitchThemeData(
        thumbColor: WidgetStateProperty.resolveWith((states) =>
        states.contains(WidgetState.selected) ? Colors.white : const Color(0xFF94A3B8)),
        trackColor: WidgetStateProperty.resolveWith((states) =>
        states.contains(WidgetState.selected)
            ? primary
            : const Color(0xFFCBD5E1)),
      ),

      floatingActionButtonTheme: FloatingActionButtonThemeData(
        backgroundColor: primary,
        foregroundColor: Colors.white,
        elevation: 2,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(16),
        ),
      ),
    );
  }
}
