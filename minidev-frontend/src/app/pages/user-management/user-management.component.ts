import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService, User, UserRole, UserRequest } from '../../core/services/auth.service';

@Component({
  selector: 'app-user-management',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './user-management.component.html',
  styleUrl: './user-management.component.scss'
})
export class UserManagementComponent implements OnInit {
  private authService = inject(AuthService);

  users = signal<User[]>([]);
  loading = signal(false);
  error = signal<string | null>(null);

  // Form state
  showForm = signal(false);
  isEditing = signal(false);
  editingUserId = signal<string | null>(null);
  currentUser = signal<UserRequest>({
    username: '',
    password: '',
    displayName: '',
    role: 'USER'
  });

  roles: UserRole[] = ['ADMIN', 'USER', 'GUEST'];

  ngOnInit() {
    this.loadUsers();
  }

  loadUsers() {
    this.loading.set(true);
    this.authService.getUsers().subscribe({
      next: (users) => {
        this.users.set(users);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set('Error loading users');
        this.loading.set(false);
      }
    });
  }

  openCreateForm() {
    this.isEditing.set(false);
    this.editingUserId.set(null);
    this.currentUser.set({
      username: '',
      password: '',
      displayName: '',
      role: 'USER'
    });
    this.showForm.set(true);
  }

  openEditForm(user: User) {
    this.isEditing.set(true);
    this.editingUserId.set(user.id);
    this.currentUser.set({
      username: user.username,
      password: '', // Don't show password
      displayName: user.displayName,
      role: user.role
    });
    this.showForm.set(true);
  }

  cancelForm() {
    this.showForm.set(false);
  }

  saveUser() {
    const user = this.currentUser();
    const userId = this.editingUserId();
    if (this.isEditing() && userId) {
      this.authService.updateUser(userId, user).subscribe({
        next: () => {
          this.loadUsers();
          this.showForm.set(false);
        },
        error: () => this.error.set('Error updating user')
      });
    } else {
      this.authService.createUser(user).subscribe({
        next: () => {
          this.loadUsers();
          this.showForm.set(false);
        },
        error: () => this.error.set('Error creating user')
      });
    }
  }

  deleteUser(id: string) {
    if (confirm('Are you sure you want to delete this user?')) {
      this.authService.deleteUser(id).subscribe({
        next: () => this.loadUsers(),
        error: () => this.error.set('Error deleting user')
      });
    }
  }
}
