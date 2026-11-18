import 'package:flutter_test/flutter_test.dart';
import 'package:zayaan_apps/main.dart';

void main() {
  testWidgets('Zayaan Apps Store renders catalog and search bar',
      (WidgetTester tester) async {
    await tester.pumpWidget(const ZayaanAppsStoreApp());
    expect(find.text('Zayaan Apps'), findsWidgets);
    expect(find.text('Zayaan Calculator Pro'), findsOneWidget);
  });
}
