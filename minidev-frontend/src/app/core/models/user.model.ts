export type UserRole = 'ADMIN' | 'USER' | 'GUEST';

export interface User {
  id: string;
  username: string;
  displayName: string;
  role: UserRole;
  token?: string;
}

export interface UserRequest {
  username: string;
  password?: string;
  displayName?: string;
  role?: UserRole;
}
