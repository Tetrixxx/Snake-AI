javac -cp game_engine.jar Agents.java
$results = @()

$seeds = 1..10 | ForEach-Object { Get-Random -Minimum 1000000000 -Maximum 9999999999 }

foreach ($seed in $seeds) {
    Write-Host "`n`n### Jatek futtatasa a kovetkezo seeddel: $seed"

    $output = java -jar game_engine.jar 0 game.snake.SnakeGame $seed 15 25 10000 Agents

    Write-Host "$output"

    $output = java -jar game_engine.jar 0 game.snake.SnakeGame $seed 15 25 10000 Agents
    $parts = $output -split "\s+"
    $score = [float]$parts[4]
    Write-Host "Pontszam: $score"

    $results += [pscustomobject]@{ Seed = $seed; Score = $score }
}

$best = $results | Sort-Object Score | Select-Object -First 1
$worst = $results | Sort-Object Score -Descending | Select-Object -First 1
$average = ($results | Measure-Object Score -Average).Average

Write-Host "`nLegjobb pontszam: $($best.Score) ezzel a seeddel: $($best.Seed)"
Write-Host "Legrosszabb pontszam: $($worst.Score) ezzel a seeddel: $($worst.Seed)"
Write-Host "Atlag pontszam: $([math]::Round($average, 2))"
