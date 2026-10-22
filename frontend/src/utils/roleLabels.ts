const roleLabels: Record<string, string> = {
  ADMIN: 'Administração',
  RH_TESOURARIA: 'RH / Tesouraria',
  DIACONO: 'Voluntário ADM',
}

export function formatRole(role: string): string {
  return roleLabels[role.replace('ROLE_', '')] ?? role.replace('ROLE_', '')
}
