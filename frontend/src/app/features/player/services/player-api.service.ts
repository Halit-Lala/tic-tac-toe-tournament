import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Player } from '../model/player.model';

@Injectable({ providedIn: 'root' })
export class PlayerApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/players';

  search(term: string): Observable<Player[]> {
    return this.http.get<Player[]>(this.baseUrl, {
      params: new HttpParams().set('search', term),
    });
  }
}