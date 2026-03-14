
import 'package:client/features/auth/presentation/bloc/auth_bloc.dart';
import 'package:client/features/auth/presentation/widgets/auth_widgets.dart';
import 'package:client/features/auth/presentation/widgets/password_field.dart';
import 'package:client/shared/widgets/index.dart';
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:go_router/go_router.dart';

class _Brand {
  static const primary = Color(0xFF00D6AB);
  static const bgDark = Color(0xFF0F231F);
  static const cardDark = Color(0x1A00D6AB); // primary/10
  static const borderSubtle = Color(0x2600D6AB); // primary/15
  static const textMuted = Color(0xFF94A3B8); // slate-400
}

class LoginPage extends StatefulWidget {
  const LoginPage({super.key});

  @override
  State<LoginPage> createState() => _LoginPageState();
}

class _LoginPageState extends State<LoginPage>
    with SingleTickerProviderStateMixin {
  final _formKey = GlobalKey<FormState>();
  final _emailController = TextEditingController();
  final _passwordController = TextEditingController();
  final _emailFocus = FocusNode();
  final _passwordFocus = FocusNode();

  late final AnimationController _fadeCtrl;
  late final Animation<double> _fadeAnim;

  @override
  void initState() {
    super.initState();
    _fadeCtrl = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 600),
    )..forward();
    _fadeAnim = CurvedAnimation(parent: _fadeCtrl, curve: Curves.easeOut);
  }

  @override
  void dispose() {
    _emailController.dispose();
    _passwordController.dispose();
    _emailFocus.dispose();
    _passwordFocus.dispose();
    _fadeCtrl.dispose();
    super.dispose();
  }

  void _submit() {
    FocusScope.of(context).unfocus();
    if (!_formKey.currentState!.validate()) return;
    context.read<AuthBloc>().add(
      AuthLoginRequested(
        email: _emailController.text.trim(),
        password: _passwordController.text,
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: _Brand.bgDark,
      body: Stack(
        children: [
          //  Ambient glow decorations
          const AmbientGlow(),

          SafeArea(
            child: FadeTransition(
              opacity: _fadeAnim,
              child: Center(
                child: SingleChildScrollView(
                  padding: const EdgeInsets.symmetric(
                      horizontal: 24, vertical: 32),
                  child: ConstrainedBox(
                    constraints: const BoxConstraints(maxWidth: 440),
                    child: Column(
                      children: [
                        //  Brand header
                        const BrandHeader(),
                        const SizedBox(height: 36),

                        //  Glass card
                        GlassCard(
                          child: Form(
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

                                // Email
                                const CustomFormLabel('Work Email'),
                                const SizedBox(height: 8),
                                CustomTextField(
                                  controller: _emailController,
                                  focusNode: _emailFocus,
                                  hintText: 'name@company.com',
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
                                const SizedBox(height: 20),

                                // Password row
                                Row(
                                  mainAxisAlignment:
                                  MainAxisAlignment.spaceBetween,
                                  children: [
                                    const CustomFormLabel('Password'),
                                    GestureDetector(
                                      onTap: () =>
                                          context.push('/forgot-password'),
                                      child: const Text(
                                        'Forgot Password?',
                                        style: TextStyle(
                                          color: _Brand.primary,
                                          fontSize: 12,
                                          fontWeight: FontWeight.w700,
                                        ),
                                      ),
                                    ),
                                  ],
                                ),
                                const SizedBox(height: 8),
                                PasswordField(
                                  controller: _passwordController,
                                  focusNode: _passwordFocus,
                                  hintText: 'Enter your password',
                                  textInputAction: TextInputAction.done,
                                  onFieldSubmitted: (_) => _submit(),
                                  validator: (v) {
                                    if (v == null || v.isEmpty) {
                                      return 'Password is required';
                                    }
                                    return null;
                                  },
                                ),
                                const SizedBox(height: 28),

                                // Submit
                                BlocBuilder<AuthBloc, AuthState>(
                                  buildWhen: (p, c) =>
                                  p.isLoading != c.isLoading,
                                  builder: (context, state) =>
                                      SubmitButton(
                                        label: 'Log In to Agile Butler',
                                        icon: Icons.login_rounded,
                                        isLoading: state.isLoading,
                                        onPressed: _submit,
                                      ),
                                ),

                                // Divider
                                const SizedBox(height: 28),
                                const OrDivider(),
                                const SizedBox(height: 20),

                                // Social buttons
                                Row(
                                  children: [
                                    Expanded(
                                      child: SocialButton(
                                        label: 'Google',
                                        icon: Icons.g_mobiledata_rounded,
                                        onPressed: () {},
                                      ),
                                    ),
                                    const SizedBox(width: 12),
                                    Expanded(
                                      child: SocialButton(
                                        label: 'GitHub',
                                        icon: Icons.code_rounded,
                                        onPressed: () {},
                                      ),
                                    ),
                                  ],
                                ),
                              ],
                            ),
                          ),
                        ),

                        //  Footer
                        const SizedBox(height: 24),
                        Row(
                          mainAxisAlignment: MainAxisAlignment.center,
                          children: [
                            const Text(
                              "Don't have an account yet?",
                              style: TextStyle(
                                color: _Brand.textMuted,
                                fontSize: 14,
                              ),
                            ),
                            TextButton(
                              onPressed: () => context.push('/register'),
                              style: TextButton.styleFrom(
                                foregroundColor: _Brand.primary,
                                padding:
                                const EdgeInsets.symmetric(horizontal: 6),
                              ),
                              child: const Text(
                                'Request Access',
                                style: TextStyle(fontWeight: FontWeight.w700),
                              ),
                            ),
                          ],
                        ),
                      ],
                    ),
                  ),
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}
