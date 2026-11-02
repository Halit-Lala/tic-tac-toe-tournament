// setup-tournament.ts
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { Router } from '@angular/router';
import { EMPTY, Observable, Subject, catchError, exhaustMap, finalize, map } from 'rxjs';
import { AutocompleteInput } from '../../../../shared/components/autocomplete-input/autocomplete-input';
import { distinctNamesValidator, notBlankValidator } from '../../validators/name-validators';
import { TournamentApiService } from '../../../tournament/services/tournament-api.service';
import { PlayerApiService } from '../../../player/services/player-api.service';
import { Tournament, TournamentFormat } from '../../../tournament/models/tournament.model';
import { NewTournamentRequest } from '../../models/setup.model';
import { Player } from '../../../player/model/player.model';

@Component({
  selector: 'app-setup-tournament',
  imports: [ReactiveFormsModule, MatButtonToggleModule, MatButtonModule, AutocompleteInput],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './setup-tournament.html',
  styleUrl: './setup-tournament.scss',
})
export class SetupTournament {
  private readonly fb = inject(FormBuilder);
  private readonly tournamentApi = inject(TournamentApiService);
  private readonly playerApi = inject(PlayerApiService);
  private readonly router = inject(Router);

  protected readonly maxNameLength = 50;
  protected readonly formats: Array<{ value: TournamentFormat; label: string }> = [
    { value: 'BEST_OF_1', label: 'Best of 1' },
    { value: 'BEST_OF_3', label: 'Best of 3' },
    { value: 'BEST_OF_5', label: 'Best of 5' },
  ];

  protected readonly submitting = signal(false);

  protected readonly form = this.fb.nonNullable.group(
    {
      playerXName: ['', [Validators.required, notBlankValidator, Validators.maxLength(this.maxNameLength)]],
      playerOName: ['', [Validators.required, notBlankValidator, Validators.maxLength(this.maxNameLength)]],
      format: ['BEST_OF_3' as TournamentFormat, Validators.required],
    },
    { validators: distinctNamesValidator },
  );

  protected readonly searchPlayers = (term: string): Observable<Player[]> => this.playerApi.search(term);

  private readonly submit$ = new Subject<NewTournamentRequest>();

  constructor() {
    this.submit$.pipe(
        exhaustMap((request) =>
          this.tournamentApi.create(request).pipe(
            finalize(() => this.submitting.set(false)),
            catchError(() => EMPTY),
          ),
        ),
        takeUntilDestroyed(),
      )
      .subscribe((tournament: Tournament) => this.router.navigate(['/tournaments', tournament.id]));
  }

  protected onSubmit(): void {
    if (this.form.invalid || this.submitting()) {
      return;
    }
    this.submitting.set(true);

    const newTournament: NewTournamentRequest = this.form.getRawValue();
    this.submit$.next({ playerXName: newTournament.playerXName.trim(), playerOName: newTournament.playerOName.trim(), format: newTournament.format });
  }
}