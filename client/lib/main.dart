import 'package:client/app/app.dart';
import 'package:client/core/constants/app_config.dart';
import 'package:client/di/injection.dart';
import 'package:flutter/material.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await configureDependencies(config: AppConfig.dev);
  runApp(const App());
}

