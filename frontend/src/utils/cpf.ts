export function maskCpf(cpf?: string | null): string {
  if (!cpf) return '-'
  const digits = cpf.replace(/\D/g, '')
  if (digits.length === 11) {
    return `***.***.${digits.slice(6, 9)}-${digits.slice(9)}`
  }
  return cpf.replace(/(\d{3})\.(\d{3})\.(\d{3})-(\d{2})/, '***.***.$3-$4')
}

export function formatCpfInput(value: string): string {
  const cleaned = value.replace(/\D/g, '').slice(0, 11)
  return cleaned
    .replace(/(\d{3})(\d)/, '$1.$2')
    .replace(/(\d{3})(\d)/, '$1.$2')
    .replace(/(\d{3})(\d{1,2})$/, '$1-$2')
}
