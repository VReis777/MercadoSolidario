interface ApiErrorLike {
  message?: string
  response?: {
    status?: number
    data?: {
      message?: string
    }
  }
}

export function getApiErrorMessage(error: unknown, fallback: string): string {
  const err = error as ApiErrorLike
  const status = err?.response?.status
  const serverMessage = err?.response?.data?.message

  if (status === 401) {
    return 'Sua sessão expirou. Faça login novamente.'
  }
  if (status === 403) {
    return 'Você não possui permissão para esta operação.'
  }
  if (status === 404) {
    return serverMessage || 'Registro não encontrado.'
  }
  if (status === 409) {
    return serverMessage || 'A operação não pode ser concluída por conflito com o estado atual.'
  }
  if (status === 400) {
    return serverMessage || 'Dados inválidos. Verifique os campos e tente novamente.'
  }
  if (status === 500) {
    return 'Ocorreu um erro interno. Tente novamente mais tarde.'
  }
  if (status === 503) {
    return serverMessage || fallback
  }
  if (status === 422) {
    return serverMessage || 'Estoque insuficiente para esta movimentação.'
  }
  if (status) {
    return fallback
  }
  return 'Não foi possível conectar ao servidor. Verifique sua conexão e tente novamente.'
}
