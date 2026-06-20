import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../core/services/auth.service';
import { User, UserRole, UserRequest } from '../../core/models/user.model';
import { ToastService } from '../../components/shared/toast/toast.service';
import { LoadingSpinnerComponent } from '../../components/shared/loading-spinner/loading-spinner.component';

/**
 * User management page component.
 */
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

  public users = signal<User[]>([]);
  public loading = signal(true);
  public error = signal<string | null>(null);

  public showForm = signal(false);
  public isEditing = signal(false);
  public editingUserId = signal<string | null>(null);
  public formUsername = signal('');
  public formDisplayName = signal('');
  public formPassword = signal('');
  public formRole = signal<UserRole>('USER');
  public formSubmitting = signal(false);

  public deleteConfirmId = signal<string | null>(null);

  public roles: UserRole[] = ['ADMIN', 'USER', 'GUEST'];

  /**
   * Angular lifecycle hook. Loads all users on init.
   */
  public ngOnInit(): void {
    this.loadUsers();
  }

  /**
   * Loads all users from the server.
   */
  public loadUsers(): void {
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

  /**
   * Opens the create user form.
   */
  public openCreateForm(): void {
    this.isEditing.set(false);
    this.editingUserId.set(null);
    this.formUsername.set('');
    this.formDisplayName.set('');
    this.formPassword.set('');
    this.formRole.set('USER');
    this.showForm.set(true);
  }

  /**
   * Opens the edit user form pre-filled with user data.
   * @param {User} user - The user to edit.
   */
  public openEditForm(user: User): void {
    this.isEditing.set(true);
    this.editingUserId.set(user.id);
    this.formUsername.set(user.username);
    this.formDisplayName.set(user.displayName);
    this.formPassword.set('');
    this.formRole.set(user.role);
    this.showForm.set(true);
  }

  /**
   * Cancels the user form.
   */
  public cancelForm(): void {
    this.showForm.set(false);
  }

  /**
   * Saves the user (create or update).
   */
  public saveUser(): void {
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

  /**
   * Requests deletion confirmation for a user.
   * @param {User} user - The user to delete.
   */
  public requestDelete(user: User): void {
    this.deleteConfirmId.set(user.id);
  }

  /**
   * Cancels the deletion confirmation.
   */
  public cancelDelete(): void {
    this.deleteConfirmId.set(null);
  }

  /**
   * Confirms and executes user deletion.
   * @param {User} user - The user to delete.
   */
  public confirmDelete(user: User): void {
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

  /**
   * Gets the badge CSS class for a user role.
   * @param {UserRole} role - The user role.
   * @returns {string} The badge CSS class.
   */
  public getBadgeClass(role: UserRole): string {
    switch (role) {
      case 'ADMIN': return 'badge-admin';
      case 'USER': return 'badge-user';
      default: return 'badge-guest';
    }
  }
}
