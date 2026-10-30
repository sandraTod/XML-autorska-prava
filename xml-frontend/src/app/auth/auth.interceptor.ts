import { Injectable } from '@angular/core';
import { HttpEvent, HttpHandler, HttpInterceptor, HttpRequest, HttpErrorResponse } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { AuthService } from './auth.service';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  constructor(private auth: AuthService, private router: Router) {}

  intercept(request: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    const url = new URL(request.url, window.location.origin);
    const backend = url.origin === window.location.origin && url.pathname.startsWith('/api/');
    const publicRoute = ['/api/auth/prijava', '/api/auth/registracija'].includes(url.pathname);
    const token = backend && !publicRoute ? this.auth.token : null;
    const outgoing = token ? request.clone({ setHeaders: { Authorization: 'Bearer ' + token } }) : request;
    return next.handle(outgoing).pipe(catchError((error: HttpErrorResponse) => {
      if (backend && !publicRoute && error.status === 401) {
        this.auth.logout();
        void this.router.navigate(['/auth/prijava']);
      }
      return throwError(() => error);
    }));
  }
}
