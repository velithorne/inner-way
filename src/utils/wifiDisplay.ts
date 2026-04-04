export function securityLabel(capabilities: string): string {
  const c = capabilities || '';
  if (/WPA3/i.test(c)) return 'WPA3';
  if (/WPA2/i.test(c)) return 'WPA2';
  if (/WPA/i.test(c)) return 'WPA';
  if (/WEP/i.test(c)) return 'WEP';
  if (/^\[\]\s*$/.test(c) || /OPEN/i.test(c)) return 'Open';
  return '—';
}
