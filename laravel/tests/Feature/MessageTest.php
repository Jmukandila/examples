<?php

namespace Tests\Feature;

use App\Jobs\PublishMessage;
use App\Models\Message;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\Queue;
use Tests\TestCase;

class MessageTest extends TestCase
{
    use RefreshDatabase;

    public function test_health_endpoint_is_up(): void
    {
        $this->get('/up')->assertOk();
    }

    public function test_home_page_lists_published_messages(): void
    {
        Message::create(['author' => 'Ada', 'body' => 'Hello'])->forceFill(['published_at' => now()])->save();
        Message::create(['author' => 'Bob', 'body' => 'Still pending']);

        $this->get('/')
            ->assertOk()
            ->assertSee('Hello')
            ->assertDontSee('Still pending')
            ->assertSee('1 message(s) waiting');
    }

    public function test_posting_a_message_queues_it(): void
    {
        Queue::fake();

        $this->post('/messages', ['author' => 'Ada', 'body' => 'Hi there'])
            ->assertRedirect('/');

        Queue::assertPushed(PublishMessage::class);
        $this->assertDatabaseHas('messages', ['author' => 'Ada', 'published_at' => null]);
    }

    public function test_the_queued_job_publishes_the_message(): void
    {
        $this->post('/messages', ['author' => 'Ada', 'body' => 'Hi there']);

        $this->assertNotNull(Message::first()->published_at);
    }

    public function test_prune_deletes_old_messages(): void
    {
        $old = Message::create(['author' => 'Ada', 'body' => 'Old']);
        $old->forceFill(['created_at' => now()->subDays(40)])->save();
        Message::create(['author' => 'Bob', 'body' => 'New']);

        $this->artisan('messages:prune', ['--days' => 30])->assertSuccessful();

        $this->assertSame(['New'], Message::pluck('body')->all());
    }

    public function test_validation_rejects_empty_messages(): void
    {
        $this->post('/messages', ['author' => '', 'body' => ''])->assertSessionHasErrors(['author', 'body']);
    }
}
