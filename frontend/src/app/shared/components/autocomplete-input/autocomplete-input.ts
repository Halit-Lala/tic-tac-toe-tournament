import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { ErrorStateMatcher } from '@angular/material/core';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { Observable, catchError, debounceTime, distinctUntilChanged, map, of, startWith, switchMap } from 'rxjs';
import { Player } from '../../../features/player/model/player.model';

@Component({
  selector: 'app-autocomplete-input',
  imports: [ReactiveFormsModule, MatFormFieldModule, MatInputModule, MatAutocompleteModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './autocomplete-input.html',
  styleUrl: './autocomplete-input.scss',
})
export class AutocompleteInput {
  readonly control = input.required<FormControl<string>>();
  readonly label = input.required<string>();
  readonly search = input.required<(term: string) => Observable<Player[]>>();
  readonly markInvalid = input(false);

  protected readonly errorMatcher: ErrorStateMatcher = {
    isErrorState: (control) =>
      this.markInvalid() || (!!control?.invalid && (control.dirty || control.touched)),
  };

  private readonly suggestions = toSignal(
    toObservable(this.control).pipe(
      switchMap((control) => control.valueChanges),
      map((value) => value.trim()),
      debounceTime(500),
      distinctUntilChanged(),
      switchMap((term) =>
        term
          ? this.search()(term).pipe(
              catchError(() => of([] as Player[]))
            )
          : of([] as Player[])
      ),
    ),
    { initialValue: [] as Player[] },
  );
}