<?php

namespace App\Jobs;

use App\Models\Message;
use Illuminate\Contracts\Queue\ShouldQueue;
use Illuminate\Foundation\Queue\Queueable;

/**
 * PublishMessage moves a message from pending to published. It runs on the queue worker the
 * nginx-php-fpm image starts under supervisor, so a message that stays pending means no worker.
 */
class PublishMessage implements ShouldQueue
{
    use Queueable;

    public function __construct(public Message $message) {}

    public function handle(): void
    {
        $this->message->forceFill(['published_at' => now()])->save();
    }
}
