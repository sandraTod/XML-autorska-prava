import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { AuthGuard } from './auth/auth.guard';
import { PocetnaComponent } from './pocetna/pocetna.component';

const routes: Routes = [
  { path: 'auth', loadChildren: () => import('./auth/auth.module').then(module => module.AuthModule) },
  { path: '', component: PocetnaComponent, pathMatch: 'full', canActivate: [AuthGuard], data: { roles: ['GRADJANIN', 'SLUZBENIK'] } },
  { path: '**', redirectTo: '' }
];

@NgModule({ imports: [RouterModule.forRoot(routes)], exports: [RouterModule] })
export class AppRoutingModule {}
