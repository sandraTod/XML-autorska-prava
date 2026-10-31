import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { AuthGuard } from './auth/auth.guard';
import { PocetnaComponent } from './pocetna/pocetna.component';
import { MojiZahteviComponent } from './a1/moji-zahtevi/moji-zahtevi.component';
import { PregledZahtevaComponent } from './a1/pregled-zahteva/pregled-zahteva.component';
import { NoviZahtevComponent } from './a1/novi-zahtev/novi-zahtev.component';

const routes: Routes = [
  { path: 'novi-zahtev', component: NoviZahtevComponent, canActivate: [AuthGuard], data: { roles: ['GRADJANIN'] } },
  { path: 'moji-zahtevi', component: MojiZahteviComponent, canActivate: [AuthGuard], data: { roles: ['GRADJANIN'] } },
  { path: 'moji-zahtevi/:documentId', component: PregledZahtevaComponent, canActivate: [AuthGuard], data: { roles: ['GRADJANIN'] } },
  { path: 'auth', loadChildren: () => import('./auth/auth.module').then(module => module.AuthModule) },
  { path: '', component: PocetnaComponent, pathMatch: 'full', canActivate: [AuthGuard], data: { roles: ['GRADJANIN', 'SLUZBENIK'], redirectCitizen: true } },
  { path: '**', redirectTo: '' }
];

@NgModule({ imports: [RouterModule.forRoot(routes)], exports: [RouterModule] })
export class AppRoutingModule {}
