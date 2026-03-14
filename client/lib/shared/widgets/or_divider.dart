import 'package:flutter/material.dart';

class OrDivider extends StatelessWidget {
  const OrDivider();

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Expanded(
          child: Container(height: 1, color: const Color(0x2600D6AB)),
        ),
        const Padding(
          padding: EdgeInsets.symmetric(horizontal: 16),
          child: Text(
            'OR CONTINUE WITH',
            style: TextStyle(
              color: Color(0x99CBD5E1),
              fontSize: 10,
              fontWeight: FontWeight.w700,
              letterSpacing: 1.2,
            ),
          ),
        ),
        Expanded(
          child: Container(height: 1, color: const Color(0x2600D6AB)),
        ),
      ],
    );
  }
}
