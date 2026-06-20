export interface Page {
  id: number;
  path: string;
  title: string;
  icon: string;
  componentName: string;
  roleRequired: string | null;
  navOrder: number;
}