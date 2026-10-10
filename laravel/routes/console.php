<?php

use App\Models\Message;
use Illuminate\Support\Facades\Artisan;

Artisan::command('messages:prune {--days=30 : Delete messages older than this many days}', function () {
    $deleted = Message::where('created_at', '<', now()->subDays((int) $this->option('days')))->delete();

    $this->info("Pruned {$deleted} message(s).");
})->purpose('Delete old guestbook messages');
