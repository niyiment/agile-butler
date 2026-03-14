
import 'package:client/core/constants/app_constants.dart';
import 'package:client/features/auth/presentation/bloc/auth_bloc.dart';
import 'package:client/features/auth/presentation/widgets/password_field.dart';
import 'package:client/shared/widgets/index.dart';
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:go_router/go_router.dart';


class RegisterPage extends StatefulWidget {
  const RegisterPage({super.key});

  @override
  State<RegisterPage> createState() => _RegisterPageState();
}

class _RegisterPageState extends State<RegisterPage>
    with SingleTickerProviderStateMixin {
  final _formKey = GlobalKey<FormState>();
  final _nameController = TextEditingController();
  final _emailController = TextEditingController();
  final _passwordController = TextEditingController();
  final _confirmController = TextEditingController();

  final _nameFocus = FocusNode();
  final _emailFocus = FocusNode();
  final _passwordFocus = FocusNode();
  final _confirmFocus = FocusNode();

  late String _selectedTimezone;

  late final AnimationController _fadeCtrl;
  late final Animation<double> _fadeAnim;

  @override
  void initState() {
    super.initState();
    _selectedTimezone = _detectTimezone();
    _fadeAnim = CurvedAnimation(parent: _fadeFutureCtrl(), curve: Curves.easeOut);
  }

  Animation<double> _fadeFutureCtrl() {
    return AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 500),
    )..forward();
  }

  String _detectTimezone() {
    final tz = DateTime.now().timeZoneName;
    return AppConstants.kTimezones.contains(tz) ? tz : 'UTC';
  }

  @override
  void dispose() {
    _nameController.dispose();
    _emailController.dispose();
    _passwordController.dispose();
    _confirmController.dispose();
    _nameFocus.dispose();
    _emailFocus.dispose();
    _passwordFocus.dispose();
    _confirmFocus.dispose();
    _fadeCtrl.dispose();
    super.dispose();
  }

  void _submit() {
    FocusScope.of(context).unfocus();
    if (!_formKey.currentState!.validate()) return;

    context.read<AuthBloc>().add(
      AuthRegisterRequested(
        name: _nameController.text.trim(),
        email: _emailController.text.trim(),
        password: _passwordController.text,
        timezone: _selectedTimezone,
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Scaffold(
      backgroundColor: theme.colorScheme.surface,
      body: Stack(
        children: [
          const AmbientGlow(),
          SafeArea(
            child: FadeTransition(
              opacity: _fadeAnim,
              child: Column(
                children: [
                  //  Top app bar ─
                  _RegisterAppBar(onBack: () => context.pop()),

                  //  Scrollable body ─
                  Expanded(
                    child: SingleChildScrollView(
                      padding: const EdgeInsets.fromLTRB(24, 0, 24, 40),
                      child: ConstrainedBox(
                        constraints: const BoxConstraints(maxWidth: 480),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            // Hero banner
                            const _HeroBanner(),
                            const SizedBox(height: 28),

                            // Heading
                            const Text(
                              'Join the Mission',
                              style: TextStyle(
                                color: Color(0xFFF8FAFC),
                                fontSize: 30,
                                fontWeight: FontWeight.w800,
                                letterSpacing: -0.5,
                              ),
                            ),
                            const SizedBox(height: 6),
                            const Text(
                              'Empower your team with agile precision.',
                              style: TextStyle(
                                color: Color(0x99D6FFF4),
                                fontSize: 14,
                                fontWeight: FontWeight.w500,
                              ),
                            ),
                            const SizedBox(height: 28),

                            //  Form
                            Form(
                              key: _formKey,
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  // Error banner
                                  BlocBuilder<AuthBloc, AuthState>(
                                    buildWhen: (p, c) =>
                                    p.failure != c.failure ||
                                        p.status != c.status,
                                    builder: (context, state) {
                                      if (state.failure == null ||
                                          state.isLoading) {
                                        return const SizedBox.shrink();
                                      }
                                      return Padding(
                                        padding:
                                        const EdgeInsets.only(bottom: 20),
                                        child: ErrorBanner(
                                          message: state.failure!.message,
                                        ),
                                      );
                                    },
                                  ),

                                  // Full Name
                                  const CustomFormLabel('Full Name'),
                                  const SizedBox(height: 8),
                                  CustomTextField(
                                    controller: _nameController,
                                    focusNode: _nameFocus,
                                    hintText: 'John Doe',
                                    icon: Icons.person_outline_rounded,
                                    onFieldSubmitted: (_) =>
                                        _emailFocus.requestFocus(),
                                    validator: (v) {
                                      if (v == null || v.trim().isEmpty) {
                                        return 'Name is required';
                                      }
                                      if (v.trim().length < 2) {
                                        return 'Name must be at least 2 characters';
                                      }
                                      return null;
                                    },
                                  ),
                                  const SizedBox(height: 18),

                                  // Email
                                  const CustomFormLabel('Email'),
                                  const SizedBox(height: 8),
                                  CustomTextField(
                                    controller: _emailController,
                                    focusNode: _emailFocus,
                                    hintText: 'john@agilebutler.com',
                                    icon: Icons.mail_outline_rounded,
                                    keyboardType: TextInputType.emailAddress,
                                    onFieldSubmitted: (_) =>
                                        _passwordFocus.requestFocus(),
                                    validator: (v) {
                                      if (v == null || v.trim().isEmpty) {
                                        return 'Email is required';
                                      }
                                      if (!RegExp(r'^[^@]+@[^@]+\.[^@]+$')
                                          .hasMatch(v.trim())) {
                                        return 'Enter a valid email';
                                      }
                                      return null;
                                    },
                                  ),
                                  const SizedBox(height: 18),

                                  // Password
                                  const CustomFormLabel('Password'),
                                  const SizedBox(height: 8),
                                  PasswordField(
                                    controller: _passwordController,
                                    focusNode: _passwordFocus,
                                    hintText: '••••••••',
                                    textInputAction: TextInputAction.next,
                                    onFieldSubmitted: (_) =>
                                        _confirmFocus.requestFocus(),
                                    validator: (v) {
                                      if (v == null || v.isEmpty) {
                                        return 'Password is required';
                                      }
                                      if (v.length < 8) {
                                        return 'At least 8 characters';
                                      }
                                      if (!RegExp(r'[A-Z]').hasMatch(v)) {
                                        return 'Include an uppercase letter';
                                      }
                                      if (!RegExp(r'[0-9]').hasMatch(v)) {
                                        return 'Include a number';
                                      }
                                      return null;
                                    },
                                  ),
                                  const SizedBox(height: 18),

                                  // Confirm password
                                  const CustomFormLabel('Confirm Password'),
                                  const SizedBox(height: 8),
                                  PasswordField(
                                    controller: _confirmController,
                                    focusNode: _confirmFocus,
                                    hintText: '••••••••',
                                    onFieldSubmitted: (_) => _submit(),
                                    validator: (v) {
                                      if (v == null || v.isEmpty) {
                                        return 'Please confirm password';
                                      }
                                      if (v != _passwordController.text) {
                                        return 'Passwords do not match';
                                      }
                                      return null;
                                    },
                                  ),
                                  const SizedBox(height: 18),

                                  // Timezone
                                  const CustomFormLabel('Timezone'),
                                  const SizedBox(height: 8),
                                  _TimezoneField(
                                    selected: _selectedTimezone,
                                    onChanged: (tz) =>
                                        setState(() => _selectedTimezone = tz),
                                  ),
                                  const SizedBox(height: 28),

                                  // Submit
                                  BlocBuilder<AuthBloc, AuthState>(
                                    buildWhen: (p, c) =>
                                    p.isLoading != c.isLoading,
                                    builder: (context, state) =>
                                        SubmitButton(
                                          label: 'Create Account',
                                          icon: Icons.rocket_launch_rounded,
                                          isLoading: state.isLoading,
                                          onPressed: _submit,
                                        ),
                                  ),
                                  const SizedBox(height: 24),

                                  // Login link
                                  Row(
                                    mainAxisAlignment: MainAxisAlignment.center,
                                    children: [
                                      const Text(
                                        'Already have an account?',
                                        style: TextStyle(
                                          color: Color(0xFF94A3B8),
                                          fontSize: 14,
                                        ),
                                      ),
                                      TextButton(
                                        onPressed: () => context.pop(),
                                        style: TextButton.styleFrom(
                                          foregroundColor: theme.colorScheme.primary,
                                          padding: const EdgeInsets.symmetric(
                                              horizontal: 6),
                                        ),
                                        child: const Text(
                                          'Log in',
                                          style: TextStyle(
                                              fontWeight: FontWeight.w700),
                                        ),
                                      ),
                                    ],
                                  ),

                                  // Trust badges
                                  const SizedBox(height: 8),
                                  const _TrustBadges(),
                                  const SizedBox(height: 16),
                                ],
                              ),
                            ),
                          ],
                        ),
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}

//  App bar

class _RegisterAppBar extends StatelessWidget {

  const _RegisterAppBar({required this.onBack});
  final VoidCallback onBack;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      child: Row(
        children: [
          IconButton(
            onPressed: onBack,
            icon: const Icon(Icons.arrow_back_rounded),
            color: const Color(0xFFF8FAFC),
          ),
          const Spacer(),
          // wordmark
          Row(
            children: [
              Container(
                width: 32,
                height: 32,
                decoration: BoxDecoration(
                  color: theme.colorScheme.primary,
                  borderRadius: BorderRadius.circular(8),
                ),
                child: const Icon(
                  Icons.bolt_rounded,
                  color: Color(0xFF0F231F),
                  size: 20,
                ),
              ),
              const SizedBox(width: 8),
              const Text(
                'Agile Butler',
                style: TextStyle(
                  color: Color(0xFFF8FAFC),
                  fontSize: 16,
                  fontWeight: FontWeight.w700,
                ),
              ),
            ],
          ),
          const Spacer(),
          // Spacer to balance back button
          const SizedBox(width: 48),
        ],
      ),
    );
  }
}

//  Hero banner

class _HeroBanner extends StatelessWidget {
  const _HeroBanner();

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Container(
      width: double.infinity,
      height: 160,
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(16),
        gradient: LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: [
            theme.colorScheme.primary.withValues(alpha: 0.15),
            theme.colorScheme.primary.withValues(alpha: 0.05),
            const Color(0xFF0F231F),
          ],
        ),
        border: Border.all(color: theme.colorScheme.primary.withValues(alpha: 0.2)),
      ),
      child: Stack(
        children: [
          // Decorative grid pattern
          Positioned.fill(
            child: ClipRRect(
              borderRadius: BorderRadius.circular(15),
              child: CustomPaint(painter: _GridPainter()),
            ),
          ),
          // Centered icon cluster
          Center(
            child: Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                const _HeroIcon(Icons.security, offset: const Offset(-8, 4)),
                const SizedBox(width: 20),
                Container(
                  width: 56,
                  height: 56,
                  decoration: BoxDecoration(
                    color: theme.colorScheme.primary.withValues(alpha: 0.2),
                    borderRadius: BorderRadius.circular(14),
                    border: Border.all(
                        color: theme.colorScheme.primary.withValues(alpha: 0.4), width: 1.5),
                    boxShadow: [
                      BoxShadow(
                        color: theme.colorScheme.primary.withValues(alpha: 0.3),
                        blurRadius: 16,
                      ),
                    ],
                  ),
                  child: Icon(Icons.bolt_rounded,
                      color: theme.colorScheme.primary, size: 28),
                ),
                const SizedBox(width: 20),
                const _HeroIcon(Icons.cloud_done_outlined,
                    offset: Offset(8, -4)),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _HeroIcon extends StatelessWidget {

  const _HeroIcon(this.icon, {required this.offset});
  final IconData icon;
  final Offset offset;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Transform.translate(
      offset: offset,
      child: Container(
        width: 40,
        height: 40,
        decoration: BoxDecoration(
          color: theme.colorScheme.primary.withValues(alpha: 0.08),
          borderRadius: BorderRadius.circular(10),
          border:
          Border.all(color: theme.colorScheme.primary.withValues(alpha: 0.2)),
        ),
        child: Icon(icon, color: theme.colorScheme.primary.withValues(alpha: 0.6), size: 20),
      ),
    );
  }
}

//  Grid painter for hero

class _GridPainter extends CustomPainter {
  @override
  void paint(Canvas canvas, Size size) {
    final paint = Paint()
      ..color = const Color(0xFF00D6AB).withValues(alpha: 0.06)
      ..strokeWidth = 0.5;

    const step = 28.0;
    for (double x = 0; x < size.width; x += step) {
      canvas.drawLine(Offset(x, 0), Offset(x, size.height), paint);
    }
    for (double y = 0; y < size.height; y += step) {
      canvas.drawLine(Offset(0, y), Offset(size.width, y), paint);
    }
  }

  @override
  bool shouldRepaint(_) => false;
}

// ─ Timezone field ─

class _TimezoneField extends StatelessWidget {

  const _TimezoneField({
    required this.selected,
    required this.onChanged,
  });
  final String selected;
  final ValueChanged<String> onChanged;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final effectiveSelected =
    AppConstants.kTimezones.contains(selected) ? selected : 'UTC';

    return DropdownButtonFormField<String>(
      initialValue: effectiveSelected,
      dropdownColor: const Color(0xFF0F231F),
      style: const TextStyle(color: Colors.white, fontSize: 15),
      icon: const Icon(Icons.expand_more_rounded,
          color: Color(0x99CBD5E1), size: 20),
      isExpanded: true,
      decoration: InputDecoration(
        prefixIcon: const Icon(Icons.schedule_outlined,
            color: Color(0x99CBD5E1), size: 20),
        filled: true,
        fillColor: const Color(0x0D0F231F),
        contentPadding:
        const EdgeInsets.symmetric(horizontal: 16, vertical: 16),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide:
          const BorderSide(color: Color(0x4000D6AB)),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: BorderSide(color: theme.colorScheme.primary, width: 1.5),
        ),
        errorBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: const BorderSide(color: Color(0xFFF87171)),
        ),
        focusedErrorBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide:
          const BorderSide(color: Color(0xFFF87171), width: 1.5),
        ),
      ),
      items: AppConstants.kTimezones
          .map((tz) => DropdownMenuItem(
        value: tz,
        child: Text(tz,
            overflow: TextOverflow.ellipsis,
            style: const TextStyle(fontSize: 14)),
      ))
          .toList(),
      onChanged: (v) {
        if (v != null) onChanged(v);
      },
      validator: (v) =>
      v == null || v.isEmpty ? 'Please select a timezone' : null,
    );
  }
}

// Trust badges

class _TrustBadges extends StatelessWidget {
  const _TrustBadges();

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        _badge(Icons.security_outlined),
        const SizedBox(width: 24),
        _badge(Icons.cloud_done_outlined),
        const SizedBox(width: 24),
        _badge(Icons.verified_user_outlined),
      ],
    );
  }

  Widget _badge(IconData icon) {
    return Icon(icon, color: const Color(0x4094A3B8), size: 22);
  }
}
