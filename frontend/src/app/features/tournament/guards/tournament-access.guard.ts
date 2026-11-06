import { CanActivateFn, Router } from "@angular/router";
import { TournamentApiService } from "../services/tournament-api.service";
import { inject } from "@angular/core";
import { SeatStorage } from "../services/seat-storage.service";
import { NotificationService } from "../../../core/service/notification.service";
import { catchError, map, of, tap } from "rxjs";

export const tournamentAccessGuard: CanActivateFn = (route) => {
  const tournamentApi = inject(TournamentApiService);
  const seatStorage = inject(SeatStorage);
  const router = inject(Router);
  const notifications = inject(NotificationService);

  const id = Number(route.paramMap.get('id'));
  
  if (seatStorage.getSeatAccess(id)) return true;
  
  const results = router.createUrlTree(['/tournaments', id, 'results']);

  const invite = new URLSearchParams(route.fragment ?? '').get('invite');
  if (invite) {
    return tournamentApi.joinTournament(id, invite).pipe(
      tap((access) => seatStorage.storeSeatAccess(id, access)),
      map(() => router.createUrlTree(['/tournaments', id])),
      catchError(() => {
        notifications.info('This invite is no longer valid. You can follow the tournament here.');
        return of(results);
      }),
    );
  }

  return tournamentApi.get(id, { notifyErrors: false }).pipe(
    map((t) => (t.mode === 'LOCAL' ? true : results)),
    catchError(() => of(true)),
  );
};