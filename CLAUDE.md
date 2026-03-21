# Darts App – Entwicklungshinweise

## UI / Layout
- **Querformat ist das primäre Ziel.** Alle UI-Beschreibungen und Entwürfe beziehen sich auf Landscape.
- Hochformat soll weiterhin funktionieren, ist aber sekundär.
- Layouts so gestalten, dass sie im Querformat optimal aussehen und im Hochformat graceful degraden (z.B. über `WindowSizeClass` oder adaptive Layouts).
