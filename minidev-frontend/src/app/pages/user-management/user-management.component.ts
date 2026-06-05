import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../core/services/auth.service';
import { User, UserRole, UserRequest } from '../../core/models/user.model';
import { ToastService } from '../../components/shared/toast/toast.service';
import { LoadingSpinnerComponent } from '../../components/shared/loading-spinner/loading-spinner.component';

@Component({
  selector: 'app-user-management',
  standalone: true,
  imports: [CommonModule, FormsModule, LoadingSpinnerComponent],
  templateUrl: './user-management.component.html',
  styleUrl: './user-management.component.scss'
})
export class UserManagementComponent implements OnInit {
  private authService = inject(AuthService);
  private toast = inject(ToastService);

  users = signal<User[]>([]);
  loading = signal(true);
  error = signal<string | null>(null);

  showForm = signal(false);
  isEditing = signal(false);
  editingUserId = signal<string | null>(null);
  formUsername = signal('');
  formDisplayName = signal('');
  formPassword = signal('');
  formRole = signal<UserRole>('USER');
  formSubmitting = signal(false);

  deleteConfirmId = signal<string | null>(null);

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
      error: () => {
        this.error.set('Failed to load users');
        this.loading.set(false);
      }
    });
  }

  openCreateForm() {
    this.isEditing.set(false);
    this.editingUserId.set(null);
    this.formUsername.set('');
    this.formDisplayName.set('');
    this.formPassword.set('');
    this.formRole.set('USER');
    this.showForm.set(true);
  }

  openEditForm(user: User) {
    this.isEditing.set(true);
    this.editingUserId.set(user.id);
    this.formUsername.set(user.username);
    this.formDisplayName.set(user.displayName);
    this.formPassword.set('');
    this.formRole.set(user.role);
    this.showForm.set(true);
  }

  cancelForm() {
    this.showForm.set(false);
  }

  saveUser() {
    if (!this.formUsername()) return;

    const userReq: UserRequest = {
      username: this.formUsername(),
      displayName: this.formDisplayName(),
      role: this.formRole(),
    };

    if (this.formPassword()) {
      userReq.password = this.formPassword();
    }

    this.formSubmitting.set(true);

    const request = this.isEditing() && this.editingUserId()
      ? this.authService.updateUser(this.editingUserId()!, userReq)
      : this.authService.createUser(userReq);

    request.subscribe({
      next: () => {
        this.toast.success(this.isEditing() ? 'User updated' : 'User created');
        this.loadUsers();
        this.showForm.set(false);
        this.formSubmitting.set(false);
      },
      error: () => {
        this.toast.error(this.isEditing() ? 'Failed to update user' : 'Failed to create user');
        this.formSubmitting.set(false);
      }
    });
  }

  requestDelete(user: User) {
    this.deleteConfirmId.set(user.id);
  }

  cancelDelete() {
    this.deleteConfirmId.set(null);
  }

  confirmDelete(user: User) {
    this.deleteConfirmId.set(null);
    this.authService.deleteUser(user.id).subscribe({
      next: () => {
        this.toast.success(`User "${user.username}" deleted`);
        this.loadUsers();
      },
      error: () => {
        this.toast.error('Failed to delete user');
      }
    });
  }

  getBadgeClass(role: UserRole): string {
    switch (role) {
      case 'ADMIN': return 'badge-admin';
      case 'USER': return 'badge-user';
      default: return 'badge-guest';
    }
  }
}
