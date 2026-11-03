import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { Tournament } from '../../models/tournament.model';

@Component({
  selector: 'app-scoreboard',
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './scoreboard.html',
  styleUrl: './scoreboard.scss',
})
export class Scoreboard {
  readonly tournament = input.required<Tournament>();
}