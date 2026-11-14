export interface Curso {
  id: number;
  codigo: string;
  nombre: string;
  creditos: number;
  docente: string;
  cupos: number;
  activo: boolean;
}

export type CursoRequest = Omit<Curso, 'id' | 'activo'>;

export interface Pagina<T> {
  contenido: T[];
  pagina: number;
  tamanio: number;
  totalElementos: number;
  totalPaginas: number;
  ultima: boolean;
}

// Estos nombres coinciden con TokenResponse del backend del Laboratorio 2.
export interface TokenResponse {
  accessToken: string;
  tokenType: 'Bearer';
  expiresIn: number;
  username: string;
  roles: string[];
}

export interface ProblemDetail {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  instance?: string;
  errores?: Record<string, string>;
}
