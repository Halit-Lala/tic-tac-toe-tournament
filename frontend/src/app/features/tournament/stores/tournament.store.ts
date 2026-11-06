import { computed, inject, Injectable, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { EMPTY, Subject, catchError, distinctUntilChanged, exhaustMap, filter, finalize, merge, switchMap, tap } from 'rxjs';
import { CreatedTournament, SeatAccess, TOTAL_GAMES, Tournament } from '../models/tournament.model';
import { TournamentApiService } from '../services/tournament-api.service';
import { SeatStorage } from '../services/seat-storage.service';
import { TournamentLiveService } from '../services/tournament-live.service';
import { NavigationEnd, Router } from '@angular/router';

@Injectable({ providedIn: 'root' })
export class TournamentStore {
    private readonly tournamentApi = inject(TournamentApiService);
    private readonly seatStorage = inject(SeatStorage);
    private readonly tournamentLiveService = inject(TournamentLiveService);
    private readonly router = inject(Router);

    private readonly _tournament = signal<Tournament | null>(null);
    private readonly _access = signal<SeatAccess | null>(null);
    private readonly _loading = signal(false);
    private readonly _busy = signal(false);
    private readonly _loadFailed = signal(false);

    readonly tournament = this._tournament.asReadonly();
    readonly loading = this._loading.asReadonly();
    readonly busy = this._busy.asReadonly();
    readonly loadFailed = this._loadFailed.asReadonly();

    readonly mySeat = computed(
        () => this._access()?.seat ?? null);
    
    readonly inviteToken = computed(
        () => this._access()?.inviteToken ?? null);
    
    readonly isParticipant = computed(
        () => this._tournament()?.mode !== 'ONLINE' || this.mySeat() !== null);

    readonly canPlay = computed(
        () => {
            const tournament = this._tournament();
            const game = this.currentGame();
            if (!tournament || !game) return false;
            if (tournament.mode !== 'ONLINE') return true;
            return !tournament.awaitingOpponent && this.mySeat() !== null && game.nextSymbol === this.mySeat();
        });

    readonly lastGame = computed(
        () => this._tournament()?.games.at(-1) ?? null);

    readonly currentGame = computed(() => {
        const lastGame = this.lastGame();
        return lastGame?.status === 'IN_PROGRESS' ? lastGame : null;
    });

    // readonly currentGame = computed(
    //     () => this._tournament()?.games.find((game) => game.status === 'IN_PROGRESS') ?? null,
    // );
    
    readonly isCompleted = computed(
        () => this._tournament()?.status === 'COMPLETED');
    
    readonly awaitingNextGame = computed(
        () => this._tournament()?.status === 'IN_PROGRESS' && this.currentGame() === null);
    
    readonly progress = computed(() => {
        const tournament = this._tournament();
        return tournament ? { gameNumber: tournament.games.length, totalGames: TOTAL_GAMES[tournament.format] } : null;
    });

    private readonly load$ = new Subject<number>();
    private readonly tournamentIdToWatch$ = new Subject<number | null>();
    private readonly move$ = new Subject<{ row: number; col: number }>();
    private readonly nextGame$ = new Subject<void>();

    constructor() {
        // leaving the tournament area closes the socket
        this.router.events.pipe(
            filter((e): e is NavigationEnd => e instanceof NavigationEnd),
            filter((e) => !e.urlAfterRedirects.startsWith('/tournaments/')),
            takeUntilDestroyed(),
        ).subscribe(() => this.tournamentIdToWatch$.next(null));

        this.load$.pipe(
            tap(() => {
                this._loading.set(true);
                this._loadFailed.set(false);
            }),
            switchMap((id) =>
                this.tournamentApi.get(id).pipe(
                    tap((tournament) => {
                        this.applyIncoming(tournament);
                        this.tournamentIdToWatch$.next(tournament.id);
                    }),
                    catchError(() => {
                        this._tournament.set(null);
                        this._loadFailed.set(true);
                        return EMPTY;
                    }),
                    finalize(() => this._loading.set(false)),
                ),
            ),
            takeUntilDestroyed(),
        ).subscribe();

        this.tournamentIdToWatch$.pipe(
            distinctUntilChanged(),
            switchMap((id) =>
                id === null
                ? EMPTY
                : merge(
                    // Receive real-time tournament updates
                    this.tournamentLiveService.watch(id).pipe(
                        tap((t) => this.applyIncoming(t))
                    ),
                    // after every (re)connect, resync from REST so no message is ever missed
                    this.tournamentLiveService.connected$.pipe(
                        switchMap(() =>
                            this.tournamentApi.get(id).pipe(
                                tap((tournament) => this.applyIncoming(tournament)),
                                catchError(() => EMPTY)
                            ),
                        ),
                    ),
                ),
            ),
            takeUntilDestroyed(),
            ).subscribe();

        this.move$.pipe(
            exhaustMap(({ row, col }) => {
                const tournament = this._tournament();
                const game = this.currentGame();
                if (!tournament || !game?.nextSymbol || !this.canPlay()) {
                    return EMPTY;
                }
                this._busy.set(true);

                return this.tournamentApi.playMove(
                    tournament.id,
                    game.gameNumber,
                    { row, col, symbol: tournament.mode === 'ONLINE' ? undefined : game.nextSymbol }).pipe(
                        tap((updated) => this._tournament.set(updated)),
                        catchError(() => {
                            return EMPTY;
                        }),
                        finalize(() => this._busy.set(false)),
                    );
            }),
            takeUntilDestroyed(),
        ).subscribe();

        this.nextGame$.pipe(
            exhaustMap(() => {
                const tournament = this._tournament();
                if (!tournament) {
                    return EMPTY;
                }
                this._busy.set(true);
                return this.tournamentApi.startNextGame(tournament.id).pipe(
                    tap((updated) => this._tournament.set(updated)),
                    catchError(() => EMPTY),
                    finalize(() => this._busy.set(false)),
                );
            }),
            takeUntilDestroyed(),
        ).subscribe();
    }

    load(id: number): void {
        this._access.set(this.seatStorage.getSeatAccess(id) ?? null);
        this.load$.next(id);
    }

    playMove(row: number, col: number): void {
        this.move$.next({ row, col });
    }

    startNextGame(): void {
        this.nextGame$.next();
    }

    private applyIncoming(incoming: Tournament): void {
        const current = this._tournament();
        if (current && current.id === incoming.id && incoming.revision < current.revision) return;
        this._tournament.set(incoming);
    }

    reset(): void {
        this._tournament.set(null);
        this._access.set(null);
        this._loadFailed.set(false);
    }
}