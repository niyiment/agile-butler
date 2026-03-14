import 'package:flutter/material.dart';

class CustomFormLabel extends StatelessWidget {
  final String text;

  const CustomFormLabel(this.text);

  @override
  Widget build(BuildContext context) {
    return Text(
      text,
      style: const TextStyle(
        color: Color(0xFFCBD5E1), // slate-300
        fontSize: 13,
        fontWeight: FontWeight.w600,
      ),
    );
  }
}